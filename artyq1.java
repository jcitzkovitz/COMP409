import java.awt.image.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.*;


// By Artsiom Skliar (260746797)

public class q1 {

    // Number of threads to use
    public static int threads = 1;

    //Global variables.
    public static BufferedImage outputimage;
    public static BufferedImage img;
    public static int width;
    public static int height;
    public static int[][] kernel;
    public static int[] position;



    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                //Changed this array index to 0 instead of 1, as given in the template.
                threads = Integer.parseInt(args[0]);
            }

            //Initialize first position.
            position = new int[]{0,0};

            // read in an image from a file
            img = ImageIO.read(new File("image.jpg"));

            // store the dimensions locally for convenience
            // changed to global storage.
            width = img.getWidth();
            height = img.getHeight();


            //Initialize kernel
            kernel = new int[][]{{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}};


            // create an output image
            outputimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // ------------------------------------
            // Your code would go here


            // The easiest mechanisms for getting and setting pixels are the
            // BufferedImage.setRGB(x,y,value) and getRGB(x,y) functions.
            // Note that setRGB is synchronized (on the BufferedImage object).
            // Consult the javadocs for other methods.

            // The getRGB/setRGB functions return/expect the pixel value in ARGB format, one byte per channel.  For example,
            //  int p = img.getRGB(x,y);
            // With the 32-bit pixel value you can extract individual colour channels by shifting and masking:
            //  int red = ((p>>16)&0xff);
            //  int green = ((p>>8)&0xff);
            //  int blue = (p&0xff);
            // If you want the alpha channel value it's stored in the uppermost 8 bits of the 32-bit pixel value
            //  int alpha = ((p>>24)&0xff);
            // Note that an alpha of 0 is transparent, and an alpha of 0xff is fully opaque.

            // ------------------------------------

            //Start the timer.
            long timerStart = System.currentTimeMillis();

            //Keep all running threads in an array.
            Thread allThreads[] = new Thread[threads];

            //Start all threads with the ConvolutionProcess.
            for (int i = 0; i < threads; i++) {
                ConvolutionProcess task = new ConvolutionProcess(position[0],position[1]);
                Thread t = new Thread(task);
                allThreads[i] = t;
                allThreads[i].start();
            }

            //Once all threads started, wait for all of them to finish before moving on.
            for (int j = 0; j < threads; j++) {
                allThreads[j].join();
            }

            //Record time.
            long timerStop = System.currentTimeMillis();

            long timeResult = timerStop - timerStart;
            System.out.println("Time to finish Convolution: " + (timerStop - timerStart));

            // Write out the image
            File outputfile = new File("outputimage.png");
            ImageIO.write(outputimage, "png", outputfile);

        } catch (Exception e) {
            System.out.println("ERROR " + e);
            e.printStackTrace();
        }

    }

    //Synchronized method to fetch latest un-processed position and increment it every time it's called.
    synchronized private static int[] getAndIncPosition() {

        //If we're in the middle of a row, simply move to next column.
        if (position[0] < width - 1) {
            position[0]++;
        }
        //If we're at the end of a row and there are more rows to go, reset column pointer and move to next row.
        else if (position[0] == width - 1 && position[1] < height) {
            position[0] = 0;
            position[1]++;
        }
        //Otherwise, we are done.
        else {
            return position;
        }
        return position;
    }

    //Static class to implement Runnable.
    private static class ConvolutionProcess implements Runnable {

        //Takes two ints to tackle position.
        private int currentX;
        private int currentY;

        private ConvolutionProcess(int x, int y) {
            this.currentX = x;
            this.currentY = y;
        }

        /**
         * This method will perform the convolution on the given parameters and will fetch a new unprocessed position
         * when it's done until every image position has been covered.
         */
        @Override
        public void run() {
            //While we still have rows to go.
            while (currentY < height) {

                //Initialize accumulators.
                int red = 0;
                int green = 0;
                int blue = 0;

                //For every position in the kernel, we get the corresponding pixel if it's within the borders of the img.
                //We calculate the weight and store it in the accumulator.
                for (int i = 0; i < kernel.length; i++) {
                    for (int j = 0; j < kernel[0].length; j++) {
                        if (currentX - 1 + j >= 0 && currentX - 1 + j < width && currentY - 1 + i >= 0 && currentY - 1 + i < height) {
                            int p = img.getRGB(currentX - 1 + j, currentY - 1 + i);
                            red += ((p >> 16) & 0xff) * kernel[i][j];
                            green += ((p >> 8) & 0xff) * kernel[i][j];
                            blue += (p & 0xff) * kernel[i][j];
                        }
                    }
                }

                //Keep the resulting values between 0 and 255.
                red = Math.max(Math.min(255, red), 0);
                blue = Math.max(Math.min(255, blue), 0);
                green = Math.max(Math.min(255, green), 0);

                //Get the new pixel value and store it in the output image.
                int argb = ((0xff) << 24) | (red << 16) | (green << 8) | (blue);
                outputimage.setRGB(currentX, currentY, argb);

                //Get the next position to cover.
                int[] nextPosition = getAndIncPosition();
                currentX = nextPosition[0];
                currentY = nextPosition[1];

            }

        }
    }
}

