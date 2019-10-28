#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>



using namespace cv;
using namespace std;




float resize(Mat img_src, Mat &img_resize, int resize_width){


    float scale = resize_width / (float)img_src.cols ;

    if (img_src.cols > resize_width) {

        int new_height = cvRound(img_src.rows * scale);

        resize(img_src, img_resize, Size(resize_width, new_height));

    }

    else {

        img_resize = img_src;

    }

    return scale;

}

extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);

}

extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertMyFilter(JNIEnv *env, jobject thiz,
                                                                jlong matAddrInput,
                                                                jlong matAddrResult) {

    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;


/* 매트릭스 회전 코드 => 아직 검증 못함
 * Point2f src_center(source.cols/2.0F, source.rows/2.0F);
    Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
    Mat dst;
    warpAffine(source, dst, rot_mat, source.size());*/




}


extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertSepiaFilter(JNIEnv *env, jobject thiz,
                                                                   jlong matAddrInput,
                                                                   jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    //세파아 필터를 적용하기 위해
    //RGBA => RGB 로 채널을 줄인다
    cvtColor(matInput, matResult, COLOR_RGBA2RGB);
    cvtColor(matInput, matInput, COLOR_RGBA2RGB);

    Mat_<float> sepia(3,3);
    sepia << .393,.769,.189  // rgb
            ,.349,.686,.168
            ,.272,.534,.131;

    transform(matInput, matResult, sepia);}

extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertInvertFilter(JNIEnv *env, jobject thiz,
                                                                    jlong matAddrInput,
                                                                    jlong matAddrResult) {

    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    //반전
    bitwise_not(matInput, matResult);}

extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertSketchFilter(JNIEnv *env, jobject thiz,
                                                                  jlong matAddrInput,
                                                                  jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;


    cvtColor(matInput,matResult,COLOR_RGBA2GRAY);
    Mat srcImage = matResult;

    Mat destImage1;
    //쓰레쉬 홀드(역치) 어느정도의 값이 넘으면 흰색 아니면 검은색으로 나눈다 (아직 정확하지 않음 - 공부중)
    adaptiveThreshold(srcImage,destImage1,255,ADAPTIVE_THRESH_MEAN_C,THRESH_BINARY,21,5);
    matResult = destImage1.clone();





}
extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertMorphologyExFilter(JNIEnv *env, jobject thiz,
                                                                          jlong matAddrInput,
                                                                          jlong matAddrResult) {


    Mat &img_input = *(Mat *) matAddrInput;
    Mat &img_result = *(Mat *) matAddrResult;
    cvtColor(img_input, img_result, COLOR_RGBA2GRAY);
    jstring result;
    std::stringstream buffer;
    Mat srcImage = img_result;

    Size size(5,5);
    Mat rectKernel = getStructuringElement(MORPH_RECT, size);
    buffer << "rectkernel =Mat rectKernel = getStructuringElement(MORPH_RECT, size); " << endl;
    buffer << rectKernel << endl;

    int iterations = 5;
    Point anchor(-1,-1);

    Mat openImage;
    morphologyEx(srcImage,openImage,MORPH_OPEN,rectKernel,anchor,iterations);
//    img_result = openImage.clone();

    Mat closeImage;
    morphologyEx(srcImage,closeImage,MORPH_CLOSE,rectKernel,anchor,iterations);
//    img_result = closeImage.clone();

    Mat gradientImage;
    morphologyEx(srcImage,gradientImage,MORPH_GRADIENT,rectKernel,anchor,iterations);
//    img_result = gradientImage.clone();

    Mat tophatImage;
    morphologyEx(srcImage,tophatImage,MORPH_TOPHAT,rectKernel,anchor,iterations);
//    img_result = tophatImage.clone();

    Mat blackhatImage;
    morphologyEx(srcImage,blackhatImage,MORPH_TOPHAT,rectKernel,anchor,iterations);
    img_result = blackhatImage.clone();

    const char *cstr = buffer.str().c_str();
    result = env->NewStringUTF(cstr);
    cvtColor(img_result,img_result, COLOR_GRAY2RGBA);
    addWeighted(img_input, 0.7, img_result, 0.9, 0, img_result);
//    img_result  =    realResult;
;
}


extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertMorphologyFilter(JNIEnv *env, jobject thiz,
                                                                        jlong matAddrInput,
                                                                        jlong matAddrResult) {



    Mat &img_input = *(Mat *) matAddrInput;
    Mat &img_result = *(Mat *) matAddrResult;
    cvtColor(img_input, img_result, COLOR_RGBA2GRAY);
    jstring result;
    std::stringstream buffer;
    Mat srcImage = img_result;

    Size size(5,5);
    Mat rectKernel = getStructuringElement(MORPH_RECT, size);
    buffer << "rectkernel =Mat rectKernel = getStructuringElement(MORPH_RECT, size); " << endl;
    buffer << rectKernel << endl;

    int iterations = 3;
    Point anchor(-1,-1);
    Mat erodeImage;
    erode(srcImage,erodeImage,rectKernel,anchor,iterations);
    //애매

//    img_result = erodeImage.clone();

    Mat dilateImage;
    dilate(srcImage,dilateImage,rectKernel,anchor,iterations);
    //애매
//    img_result = dilateImage.clone();

    Mat ellipseKernel = getStructuringElement(MORPH_ELLIPSE,size);
    buffer << "ellipseKernel = Mat ellipseKernel = getStructuringElement(MORPH_ELLIPSE,size);" << endl;
    buffer << ellipseKernel << endl;

    Mat erodeImage2;
    erode(srcImage,erodeImage2,ellipseKernel,anchor,iterations);
    //애매
//    img_result = erodeImage2.clone();

    Mat dilateImage2;
    dilate(srcImage,dilateImage2,ellipseKernel,anchor,iterations);
    //애매
//    img_result = dilateImage2.clone();

    Mat corssKernel = getStructuringElement(MORPH_CROSS,size);
    buffer << "corsskernel =Mat corssKernel = getStructuringElement(MORPH_CROSS,size);" << endl;
    buffer <<corssKernel << endl;

    Mat erodeImage3;
    erode(srcImage,erodeImage3,corssKernel,anchor,iterations);
/*    //애매
    img_result = erodeImage3.clone();
    cvtColor(img_result,img_result, COLOR_GRAY2RGBA);
    addWeighted(img_input, 0.5, img_result, 0.9, 0, img_result);*/

    Mat dilateImage3;
    dilate(srcImage,dilateImage3,corssKernel,anchor,iterations);
    //뭔지 잘 모르겠음
//    img_result = dilateImage3.clone();

    const char *cstr = buffer.str().c_str();
    result = env->NewStringUTF(cstr);

    cvtColor(img_result,img_result, COLOR_GRAY2RGBA);
    addWeighted(img_input, 0.5, img_result, 0.9, 0, img_result);

}


extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_ConvertBoxFilter(JNIEnv *env, jobject thiz,
                                                                 jlong matAddrInput,
                                                                 jlong matAddrResult) {
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
    jstring result;
    std::stringstream buffer;

    uchar dataA[]={1, 2, 4, 5, 2, 1,
                   3, 6, 6, 9, 0, 3,
                   1, 8, 3, 7, 2, 5,
                   2, 9, 8, 9, 9, 1,
                   3, 9, 8, 8, 7, 2,
                   4, 9, 9, 9, 9, 3};

/*    uchar dataA[]={0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0,
                   0, 0, 8, 8, 0, 0,
                   0, 0, 8, 8, 0, 0,
                   0, 0, 0, 0, 0, 0,
                   0, 0, 0, 0, 0, 0};*/

    Mat A(6,6,CV_8U,dataA);
    buffer << "A= " <<endl;
    buffer << A << endl;

    int border = 1;
    Mat B;
    copyMakeBorder(A,B,border,border,border,border,BORDER_REFLECT101);
    buffer << "B = " << endl;
    buffer << B << endl;

    Size ksize(border*2 +1,border*2+1); //ksize(3,3)
    Point anchor(0,0);
    Mat dst1;
    boxFilter(A,dst1,-1,ksize,anchor,false);

    buffer << "dst1 = " << endl;
    buffer << dst1 << endl;

    Mat dst2;
    boxFilter(A,dst2,-1,ksize,anchor,true);
    buffer << "dst2 = " << endl;
    buffer << dst2 << endl;

    Mat dst3;
    int d =ksize.width;
    double sigmaColor = 2.0;
    double sigmaSpace = 2.0;
    bilateralFilter(A ,dst3,3,d,sigmaColor,sigmaSpace);
    buffer << "dst3 = " << endl;
    buffer << dst3 << endl;

    Mat dst4;
    bilateralFilter(A,dst4,3,-1,sigmaColor,sigmaSpace);
    buffer << "dst4 = " <<endl;
    buffer << dst4 << endl;

    const char *cstr = buffer.str().c_str();
    result = env->NewStringUTF(cstr);
    matResult = dst2;

}






extern "C"
JNIEXPORT jlong JNICALL
Java_june_third_autoemotionsticker_MainActivity_loadCascade(JNIEnv *env, jobject thiz,
                                                            jstring cascade_file_name) {
    // TODO: implement loadCascade()
    const char *nativeFileNameString = env->GetStringUTFChars(cascade_file_name, 0);


    string baseDir("/storage/emulated/0/");

    baseDir.append(nativeFileNameString);

    const char *pathDir = baseDir.c_str();


    jlong ret = 0;

    ret = (jlong) new CascadeClassifier(pathDir);

    if (((CascadeClassifier *) ret)->empty()) {

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);

    }

    else

        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",

                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);



    env->ReleaseStringUTFChars(cascade_file_name, nativeFileNameString);


    return ret;



}



extern "C"
JNIEXPORT void JNICALL
Java_june_third_autoemotionsticker_MainActivity_detect(JNIEnv *env, jobject thiz,
                                                       jlong cascade_classifier_face,
                                                       jlong cascade_classifier_eye,
                                                       jlong matAddrInput,
                                                       jlong matAddrResult) {

    Mat &img_input = *(Mat *) matAddrInput;

    Mat &img_result = *(Mat *) matAddrResult;


    img_result = img_input.clone();


    std::vector<Rect> faces;

    Mat img_gray;


    cvtColor(img_input, img_gray, COLOR_BGR2GRAY);

    equalizeHist(img_gray, img_gray);


    Mat img_resize;

    float resizeRatio = resize(img_gray, img_resize, 640);


    //-- Detect faces

    ((CascadeClassifier *) cascade_classifier_face)->detectMultiScale( img_resize, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );



    __android_log_print(ANDROID_LOG_DEBUG, (char *) "native-lib :: ",

                        (char *) "face %d found ", faces.size());


    for (int i = 0; i < faces.size(); i++) {

        double real_facesize_x = faces[i].x / resizeRatio;

        double real_facesize_y = faces[i].y / resizeRatio;

        double real_facesize_width = faces[i].width / resizeRatio;

        double real_facesize_height = faces[i].height / resizeRatio;


        Point center( real_facesize_x + real_facesize_width / 2, real_facesize_y + real_facesize_height/2);

        ellipse(img_result, center, Size( real_facesize_width / 2, real_facesize_height / 2), 0, 0, 360,

                Scalar(255, 0, 255), 30, 8, 0);



        Rect face_area(real_facesize_x, real_facesize_y, real_facesize_width,real_facesize_height);

        Mat faceROI = img_gray( face_area );

        std::vector<Rect> eyes;


        //-- In each face, detect eyes

        ((CascadeClassifier *) cascade_classifier_eye)->detectMultiScale( faceROI, eyes, 1.1, 2, 0 |CASCADE_SCALE_IMAGE, Size(30, 30) );


        for ( size_t j = 0; j < eyes.size(); j++ )

        {

            Point eye_center( real_facesize_x + eyes[j].x + eyes[j].width/2, real_facesize_y + eyes[j].y + eyes[j].height/2 );

            int radius = cvRound( (eyes[j].width + eyes[j].height)*0.25 );

            circle( img_result, eye_center, radius, Scalar( 255, 0, 0 ), 30, 8, 0 );

        }

    }














}


