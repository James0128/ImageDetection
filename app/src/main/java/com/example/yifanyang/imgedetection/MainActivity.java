package com.example.yifanyang.imgedetection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    private Mat originalMat;
    private Bitmap currentBitmap;
    private ImageView imageView;
    static int REQUEST_READ_EXTERNAL_STORAGE = 0;
    static boolean read_external_storage_granted = false;
    private final int ACTION_PICK_PHOTO = 1;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV Status", "OpenCV loaded successfully");
                    break;
                }

                default: {

                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = (ImageView)findViewById(R.id.image_view);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
        }else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filename, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //在这里处理动作拦元素点击操作
        //只要在AndroidManifest.xml中指定父级Activity
        //动作栏会自动处理Home健与返回键的点击操作
        int  id= item.getItemId();
        //简化的IF命令，不做检查
        if (id == R.id.action_settings){
            return  true;
        }
        else if (id == R.id.open_gallery){
            if(read_external_storage_granted) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, ACTION_PICK_PHOTO);
            }else {
                return true;
            }
        }else if (id == R.id.DoG) {
            //Apply Difference of Gaussian
            DifferenceOfGaussian();
        } else if (id == R.id.CannyEdges) {
            //Apply Canny Edge Detector
            Canny();
        } else if (id == R.id.SobelFilter) {
            //Apply Sobel Filter
            Sobel();
        } else if (id == R.id.HarrisCorners) {
            //Apply Harris Corners
            HarrisCorner();
        } else if (id == R.id.HoughLines) {
            //Apply Hough Lines
            HoughLines();
        } else if (id == R.id.HoughCircles) {
            //Apply Hough Circles
            HoughCircles();
        } else if (id == R.id.Contours) {
            //Apply contours
            Contours();
        }

        return super.onOptionsItemSelected(item);
    }




    //纠正图像方向
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode ==ACTION_PICK_PHOTO && resultCode==RESULT_OK && null!= data&& read_external_storage_granted){

            Uri selectedImage= data.getData();
            String[] filePathColumn= {MediaStore.Images.Media.DATA};//获取所选图片的列索引
            //创建cursor类得到图片获取的绝对路径
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn,null,null,null);
            cursor.moveToFirst();
            //字符串picturepath包含选定图像的路径
            int columnIndex= cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            //加速图像载入
            BitmapFactory.Options options= new BitmapFactory.Options();
            options.inSampleSize = 2;

            Bitmap temp= BitmapFactory.decodeFile(picturePath,options);

            //获取方向信息
            int orientation = 0;
            try {
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                );
            } catch (IOException e) {
                e.printStackTrace();
            }

            Matrix rotate90 = new Matrix();
            rotate90.postRotate(orientation);
            Bitmap originalBitmap = rotateBitmap(temp,orientation);

            if(originalBitmap != null) {
                Bitmap tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                originalMat = new Mat(tempBitmap.getHeight(),
                        tempBitmap.getWidth(), CvType.CV_8U);
                Utils.bitmapToMat(tempBitmap, originalMat);
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false);
                imageView.setImageBitmap(currentBitmap);
            }else {
                Log.i("data", "originalBitmap is empty");
            }
        }

        }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }
    private static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix= new Matrix();
        switch (orientation){
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1,-1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1,-1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1,-1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1,-1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-270);
                break;
            default: return bitmap;
        }
        try{
        Bitmap bmRotated = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
        bitmap.recycle();
        return bmRotated;
        }catch (OutOfMemoryError e){
            e.printStackTrace();
            return null;
        }
    }
/// 霍夫变换：这是一种被广泛采用利用数学等式的参数形式
    /*
    * 一般的霍夫变换变换可以检测任何能够以参数形式等式表达的形状。通常考虑二为形状的霍夫变换
    * 直线和圆
    * */
    /*
    * 霍夫直线：图像中选取（x1,y1）,(x2,y2),对下面两个方程求解（a,m）;
    * y1=m(x1)+a;y2=m(x2)+a
    * 维护一个包含（a,m）两列表哥和一个计数值
    * */
    public void HoughLines() {
        Mat grayMat= new Mat();
        Mat cannyEdges = new Mat();
        Mat lines =new Mat();
        //将图像转换成灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat,cannyEdges,10,100);
        //第一二个参数分别是输入和输出，第三个第四个参数指定像素R和sita的解析度
        //后两个参数是一条直线上点数的阈值和最小值。点数少于该值的直线则被舍弃
        Imgproc.HoughLinesP(cannyEdges,lines,1,Math.PI/180,50,20,20);

        Mat houghLines = new Mat();
        houghLines.create(cannyEdges.rows(),cannyEdges.cols(),CvType.CV_8UC1);
        //在图像上绘制直线
        for (int i = 0 ;i<lines.cols();i++){
            double[] points = lines.get(0,i);
            double x1,y1,x2,y2;
            x1= points[0];
            y1= points[1];
            x2= points[2];
            y2= points[3];

            Point pt1 = new Point(x1,y1);
            Point pt2 = new Point(x2,y2);

            //在一幅图像上绘制直线
            Imgproc.line(houghLines,pt1,pt2,new Scalar(255,0,0),1);

        }
        //将Mat转换回位图
        Utils.matToBitmap(houghLines,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    //与霍夫直线类似，霍夫圆按照同样的步骤检测圆形，只是等式改变了
    public void HoughCircles() {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat circles= new Mat();
        //将图像转换成灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.HoughCircles(cannyEdges,circles,Imgproc.CV_HOUGH_GRADIENT
                ,1,cannyEdges.rows()/15);//,grayMat.rows()/8);
        Mat houghCircles= new Mat();
        houghCircles.create(cannyEdges.rows(),cannyEdges.cols(),CvType.CV_8UC1);

        //在图像上绘制圆形
        for (int i=0;i<circles.cols();i++){
            double[] parameters= circles.get(0,i);
            double x,y;
            int r;
            x= parameters[0];
            y= parameters[1];
            r= (int) parameters[2];

            Point center = new Point(x,y);

            //在一幅图像上绘制圆形
            Imgproc.circle(houghCircles,center,r,new Scalar(255,0,0),1);

        }
        //将mat转换称位图
        Utils.matToBitmap(houghCircles,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    /*
    * 轮廓通常以图像中的边缘来计算，但边缘和轮廓之间的细微的区别在于轮廓是闭合的，而边缘可以是任意的
    * 边缘的概念局限于点及其邻域像素，轮廓将目标作为整体进行处理
    * */
    public void Contours() {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();

        //保存所有轮廓列表
        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        //将图像转换成为灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat,cannyEdges,10,100);
        //找出轮廓
        Imgproc.findContours(cannyEdges,contourList,hierarchy
        ,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);//第一二个参数表示输入图像和轮廓列表
        //第三个参数保存层级，第四个参数指定用户需要的层级属性
        //在新的图像上绘制轮廓
        Mat contours = new Mat();
        contours.create(cannyEdges.rows(),cannyEdges.cols(),CvType.CV_8SC3);
        Random r= new Random();
        for ( int i = 0;i<contourList.size();i++){
            Imgproc.drawContours(contours,contourList,i,new Scalar(r.nextInt(255),r.nextInt(255),
                    r.nextInt(255)),-1);
        }
        //将Mat转换成为位图
        Utils.matToBitmap(contours,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    //Harris角点检测
    /*
    * 角点是两条边缘的交点或者在局部邻域中有多个显著边缘方向的点
    * Harris角点检器是在图像上使用滑动窗口计算亮度的变化。考虑到角点周围的亮度值会有很大的变化，
    * 我们尽可能地使这个值最大化
    * 求和[I(x+u,y+v)-I(x,y)]^2
    * */
    public void HarrisCorner() {
        Mat grayMat = new Mat();
        Mat corners= new Mat();
        //将图像转换成灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);

        Mat tempDst = new Mat();
        //找出角点
        Imgproc.cornerHarris(grayMat,tempDst,2,3,0.04);

        //扫一化Harris角点的输出
        Mat tempDstNorm= new Mat();
        Core.normalize(tempDst,tempDstNorm,0,255,Core.NORM_MINMAX);
        Core.convertScaleAbs(tempDstNorm,corners);
        //在新的图像上绘制角点
        Random r = new Random();
        for (int i=0 ;i<tempDstNorm.cols();i++){
            for (int j=0;j<tempDstNorm.rows();j++){
                double[] value =  tempDstNorm.get(j,i);
                if (value[0] >150)
                    Imgproc.circle(corners, new Point(i, j), 5, new Scalar(r.nextInt(255)), 2);
            }
        }
        //将Mat转化会位图
        Utils.matToBitmap(corners,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    /*
    * 与Canny边缘检测一样，我们计算像素的灰度梯度，只不过换用另一种方式。
    * 我们通过以两个3X3的核对图像做卷积来近似的计算水平和垂直方向的灰度梯值
    *
    * 1.将图像转换为灰度图像
    * 2.计算水平方向灰度梯度的绝对值
    * 3.计算垂直方向灰度梯度的绝对值
    * 4.使用上面的公式计算最终的梯度
    * */
    public void Sobel() {
        Mat grayMat= new Mat();
        Mat sobel = new Mat();//用来保存结果的Mat

        //分别用于保存梯度和绝对梯度的mat
        Mat grad_x = new Mat();
        Mat abs_grad_x=new Mat();
        Mat grad_y = new Mat();
        Mat abs_grad_y = new Mat();

        //将图像转化为灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        //计算水平方向的梯度
        Imgproc.Sobel(grayMat,grad_x,CvType.CV_16S,1,0,3,1,0);
        //计算垂直方向的梯度
        Imgproc.Sobel(grayMat,grad_y,CvType.CV_16S,0,1,3,1,0);
        //计算两个方向上的梯度绝对值
        Core.convertScaleAbs(grad_x,abs_grad_x);
        Core.convertScaleAbs(grad_y,abs_grad_y);

        //计算结果梯度
        Core.addWeighted(abs_grad_x,0.5,abs_grad_y,0.5,1,sobel);
        //将Mat转换成位图
        Utils.matToBitmap(sobel,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }
    /*
       * 分为四个步骤
       * 1.平滑图像：通过合适的模糊半径执行高斯模糊来减少图像内噪声
       * 2.计算图像梯度： 并且将梯度分类为垂直，水平和斜对角，被用于下一步中计算真真正的边缘
       * 3.非最大值抑制：检查某一像素在梯度的正方向和负方向上是否是局部最大值，如果是，则抑制该像素（即不是边缘），
       * 这是边缘细化技术，用最急剧的变换选出边缘点
       * 4.用滞后阈值化选择边缘：检查某一边缘是否明显到足以作为最终输出，最后去除不够明显的边缘
       *
       * */
    //所有灰度梯度址小于低阈值的点归为被抑制点，灰度梯度址在之间的归位弱边缘点，灰度梯度值大于高阈值点称为强边缘点
    public void Canny() {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        //将图像转换成灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(grayMat,cannyEdges,10,100);//最后两个参数代表低阈值和高阈值

        //将mat转化成为位图
        Utils.matToBitmap(cannyEdges,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }

    /*1.将给定图像转换为灰度图像，
    2.用两个不同的模糊半径对灰度图像执行高斯模糊
    3.将前一步中产生的两幅图像相减，得到一幅只包含边缘点的结果图像
    */

    //该函数可以对任意给定的图像计算边缘
    public void DifferenceOfGaussian() {
        Mat grayMat = new Mat();
        Mat blur1= new Mat();
        Mat blur2 = new Mat();
        //将图像转换成灰度
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);
        //以两个不同的模糊半径对图像做模糊处理
        Imgproc.GaussianBlur(grayMat,blur1,new Size(15,15),5);
        Imgproc.GaussianBlur(grayMat,blur2,new Size(21,21),5);
        //将两幅模糊后的图像相减
        Mat DoG= new Mat();
        Core.absdiff(blur1,blur2,DoG);
        //反转二值阈值化
        Core.multiply(DoG,new Scalar(100),DoG);
        Imgproc.threshold(DoG,DoG,20,255,Imgproc.THRESH_BINARY_INV);
        //将Mat转换回位图
        Utils.matToBitmap(DoG,currentBitmap);
        imageView.setImageBitmap(currentBitmap);
    }
}
