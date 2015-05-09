#include <stdio.h>
#include <opencv2/opencv.hpp>
#include <opencv2/features2d/features2d.hpp>
#include "opencv2/nonfree/features2d.hpp"
#include "opencv2/nonfree/nonfree.hpp"
#include <mosquitto.h>
#include <signal.h>
#include <fstream>
#include <iostream>
#include <sys/time.h>

#define NUM_ROVER 2

static volatile int keepRunning = 1;

using namespace cv;
using namespace std;

vector<vector<Point2f> > trajectories;
vector<Point2f> lastSeen;
vector<Point2f> lastGood;
vector<Point2f> curTargets;

fstream slog, r0log, r1log;
vector<Point2f> waypointsToVisit[NUM_ROVER];
vector<Point2f> waypointsVisited[NUM_ROVER];
struct mosquitto *mozzie;

struct timeval start_time;

void intHandler(int dummy) {
    keepRunning = 0;
}

double get_time() {
    struct timeval now, diff;
    if (gettimeofday(&now, NULL)) perror("Time of day");
    timersub(&now, &start_time, &diff);
    return diff.tv_sec + (double) diff.tv_usec / 1000000;
}

void publish_waypoint(int robot) {
    if (robot >= curTargets.size()) perror("ahghg");
    char msg[100], topic[100];
    sprintf(topic, "waypoint/%d", robot);
    sprintf(msg, "Waypoint %f %f\n", curTargets[robot].x, curTargets[robot].y);
    cout << "Sending::: " << msg << endl;
    int err = mosquitto_publish(mozzie, NULL, topic, strlen(msg), msg, 0, false);
    if (err != MOSQ_ERR_SUCCESS) perror("mozzie pubish fail");
    slog << (double) get_time() << ": Robot " << robot << " to " << curTargets[robot].x << " " << curTargets[robot].y << endl;
}


void CallBackFunc(int event, int x, int y, int flags, void* userdata)
{
    if  ( event == EVENT_LBUTTONDOWN )
    {
        cout << "Left button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
        waypointsToVisit[0].push_back(Point2f(x, y));
    }
    else if  ( event == EVENT_RBUTTONDOWN )
    {
        cout << "Right button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
        waypointsToVisit[1].push_back(Point2f(x, y));
    }
    else if  ( event == EVENT_MBUTTONDOWN )
    {
        cout << "Middle button of the mouse is clicked - position (" << x << ", " << y << ")" << endl;
    }
}


void process (Mat& frame, vector<Mat> des, vector<Mat> imgs, 
        vector<vector<KeyPoint> > points) {
    Mat f = frame;
    vector<KeyPoint> keypoints;
    SiftFeatureDetector detector (750);
    SiftDescriptorExtractor extractor;
    detector.detect(f, keypoints);
    Mat fDes;
    extractor.compute(f, keypoints, fDes);
    for (int i = 0; i < des.size(); i++) {
        FlannBasedMatcher matcher;
        vector <DMatch> matches, good_matches;
        matcher.match( des[i], fDes, matches);
        double max_dist = 0; double min_dist = 100;
        for( int j = 0; j < matches.size(); j++ ) { 
            double dist = matches[j].distance;
            if ( dist < min_dist ) min_dist = dist;
            if ( dist > max_dist ) max_dist = dist;
        }

        for( int j = 0; j < matches.size(); j++ ) { 
            good_matches.push_back( matches[j]);
        }
        if (good_matches.size() == 0) continue;

        Mat f2;
        //    drawMatches(imgs[i], points[i], f, keypoints, good_matches, f2, Scalar::all(-1), Scalar::all(-1), vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);

        vector<Point2f> wootPtsObj, wootPtsScn;
        vector<KeyPoint> wootPts;
        for (int j = 0; j < good_matches.size(); j++) {
            wootPts.push_back(keypoints[good_matches[j].trainIdx]);
            wootPtsObj.push_back(points[i][good_matches[j].queryIdx].pt);
            wootPtsScn.push_back(keypoints[good_matches[j].trainIdx].pt);
        }
        Mat H = findHomography(wootPtsObj, wootPtsScn, CV_RANSAC);
        vector<Point2f> obj_corners(4);
        obj_corners[0] = cvPoint(0,0);
        obj_corners[1] = cvPoint(imgs[i].cols, 0);
        obj_corners[2] = cvPoint(imgs[i].cols, imgs[i].rows);
        obj_corners[3] = cvPoint(0, imgs[i].rows);
        vector<Point2f> scn_corners(4);
        perspectiveTransform(obj_corners, scn_corners, H);

        Point2f center;
        center.x = (scn_corners[0].x + scn_corners[1].x + scn_corners[2].x + scn_corners[3].x)/4;
        center.y = (scn_corners[0].y + scn_corners[1].y + scn_corners[2].y + scn_corners[3].y)/4;

        Scalar c;
        if (i == 0) c = Scalar(255, 0, 0);
        else if (i == 1) c = Scalar(0, 255, 0);
        else c = Scalar(0, 0, 255);

        line( f, scn_corners[0], scn_corners[1], c, 4 );
        line( f, scn_corners[1], scn_corners[2], c, 4 );
        line( f, scn_corners[2], scn_corners[3], c, 4 );
        line( f, scn_corners[3], scn_corners[0], c, 4 );    

        //    drawKeypoints (f, wootPts, f, Scalar::all(-1));
        if (trajectories[i].size() == 0) {
            trajectories[i].push_back(center);
        } else {
            double dist = norm(Mat(center),Mat(lastSeen[i]));
            if (dist < 25) {
                trajectories[i].push_back(center);

                if (!waypointsToVisit[i].empty()) {
                    // Get the current waypoint for this object
                    
                    double distTarget = norm(Mat(center), Mat(curTargets[i]));
                    cout << "Dist from " << curTargets[i] << " is " << distTarget << endl;
                    if (distTarget < 40) {
                        // We reached the waypoint, get a new target
                        waypointsVisited[i].push_back(curTargets[i]);
                        curTargets[i] = waypointsToVisit[i].back();
                        waypointsToVisit[i].pop_back();
                        publish_waypoint(i);
                    }
                    circle(f, curTargets[i], 15, c, -1, 15);
                }
            }
        }
        lastSeen[i] = center;
        //for (int j = 1; j < trajectories[i].size(); j++) {
        //    circle(f, trajectories[i][j], 5, c, -1, 8);
        //}
        //    imshow("Go", f);
        //waitKey(0);
        // Print way points to visit
        Scalar toVisit(255, 255, 255);
        for (int j = 0; j < waypointsToVisit[i].size(); j++) {
            circle(f, waypointsToVisit[i][j], 5, c, -1, 8);
        }
        Scalar haveVisit(0, 0, 0);
        for (int j = 0; j < waypointsVisited[i].size(); j++) {
            circle(f, waypointsVisited[i][j], 5, haveVisit, -1, 8);
        }
    }
    
}

void my_message_callback(struct mosquitto *mosq, void *userdata, const struct mosquitto_message *message)
{
    if(message->payloadlen){
        cout << message->topic << " " <<  message->payload << endl;
    }else{
        printf("%s (null)\n", message->topic);
    }
    fflush(stdout);
}

int main(int argc, char** argv) {
    if (argc < 5) {
        printf("usage: DisplayImage.out waypoints_file run_name <num_images> (N *<Image_Path>)\n");
        return -1;
    }
    
    // Set up logs
    char server_log_name[50], rover0_log_name[50], rover1_log_name[50];
    sprintf(server_log_name, "%s_server.txt", argv[2]);
    sprintf(rover0_log_name, "%s_r0.txt", argv[2]);
    sprintf(rover1_log_name, "%s_r1.txt", argv[2]);
    slog.open(server_log_name, fstream::out);
    r0log.open(rover0_log_name, fstream::out);
    r1log.open(rover1_log_name, fstream::out);
   
    if (gettimeofday(&start_time, NULL)) {
        cout << "Get time of day failed" << endl;
        return 0;
    }

    // Turn off auto focus
    system("v4l2-ctl -c focus_auto=0");
    VideoCapture vc(0);
    if (!vc.isOpened()) {
        cout << "Could not open video reference 0" << endl;
        return -1;
    }
    
    char video_name[50];
    sprintf(video_name, "%s.mpeg", argv[2]);
    VideoWriter vw;
    //vw.open(video_name, CV_FOURCC('P','I','M','1'), 25, Size(480, 600), true);
    signal(SIGINT, intHandler);

    namedWindow("Out", 1);
    Mat q;
    vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    //   vc >> q;
    imshow("Out", q);
    waitKey(0);

    // Setup the mosquitto library
    mosquitto_lib_init();
    mozzie = mosquitto_new(NULL, true, NULL);
    if (!mozzie) {
        cout << "Failed to create new mozzie" << endl;
        return -1;
    }
    if (mosquitto_connect(mozzie, "localhost", 1883, 60) != MOSQ_ERR_SUCCESS) {
        cout << "Connect failed" << endl;
        return -1;
    }
    //if (mosquitto_subscribe(mozzie, NULL, "gps", 0) != MOSQ_ERR_SUCCESS) {
    //    cout << "Subscribe failed" << endl;
    //    return -1;
    //}
    mosquitto_message_callback_set(mozzie, my_message_callback);

    // Get the feature points for the objects to track
    vector<Mat> descrs;
    vector<vector<KeyPoint> > comparePts;
    SiftFeatureDetector detector (200);
    SiftDescriptorExtractor extractor;

    vector<Mat> compare;
    int num_track = atoi(argv[3]);
    for (int i = 0; i < num_track; i++) {
        Mat image;
        image = imread(argv[i+4], 1);
        if (!image.data) {
            cout << "Object reference " << i << " not found." << endl;
            return -1;
        }
        resize (image, image, Size(), 1, 1);
        vector<KeyPoint> pts;
        detector.detect (image, pts);
        Mat descr;
        extractor.compute(image, pts, descr);
        descrs.push_back(descr);
        compare.push_back(image);
        comparePts.push_back(pts);
        trajectories.push_back(vector<Point2f>());
        lastSeen.push_back(Point2f(0,0));
        //    drawKeypoints(image, pts, image, Scalar(0, 255, 0));
        // imshow("Object", image);
        //waitKey(0);
    }

    // Read in the way points to travel to
    fstream wps;
    wps.open(argv[1], fstream::in);
    int id, x, y;
    while (wps >> id >> x >> y) waypointsToVisit[id].push_back(Point2f(x, y)); 
    char *msg = (char*) malloc(200);
    for (int i = 0; i < num_track; ++i) {
        if (waypointsToVisit[i].empty()) break;
        curTargets.push_back(waypointsToVisit[i].back());
        waypointsToVisit[i].pop_back();
    }
    for (int i = 0; i < num_track; ++i) {
        publish_waypoint(i);
    }

    // Track objects until a sig_int is received
    //VideoWriter vw;
    int f = 0;
    setMouseCallback("Out", CallBackFunc, NULL);
    char topic[100];
    while (keepRunning) {
        Mat frame;
        vc >> frame;
        if (frame.empty()) break;    
        resize (frame, frame, Size(), 1, 1);
        //imwrite("output.jpeg", frame);
        //if (!vw.isOpened())
        //  vw.open("output.mpeg", CV_FOURCC('P','I','M','1'), 25, frame.size());
        if (f % 5 == 0) {
            process(frame, descrs, compare, comparePts);
            for (int i = 0; i < num_track; ++i) {
                sprintf(topic, "gps/%d", i);
                sprintf(msg, "GPS %f %f\n", lastSeen[i].x, lastSeen[i].y);
                cout << "Sending::: " << msg << endl;
                if (i == 0) r0log << get_time() << ": At location " << lastSeen[0].x << " " << lastSeen[0].y << endl;
                if (i == 1) r1log << get_time() << ": At location " << lastSeen[1].x << " " << lastSeen[1].y << endl;
                int err = mosquitto_publish(mozzie, NULL, topic, strlen(msg), msg, 0, false);
                if (err != MOSQ_ERR_SUCCESS) break;
                //publish_waypoint(i);
            }
            imshow("Out", frame);
        }

        cout << "\rFrame " << f++ << " processed." << flush;
        //vw << frame;
        waitKey(5);
        mosquitto_loop(mozzie, 0, 1);
    }
    cout << "\rAll frames processed!" << endl;
    return 0;
}
