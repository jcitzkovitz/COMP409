import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class q1 {

	// Number of threads to use
	public static int threads = 1;

	// Each of the below variables are shared amongst all threads, and thus are made to be global.
	public static BufferedImage outputimage;
	public static BufferedImage img;
	public static int imgHeight;
	public static int imgWidth;
	public static int[][] kernel = {{-1,-1,-1},{-1,8,-1},{-1,-1,-1}};

	public static void main(String[] args) {
		try {
			if (args.length>0) {
				threads = Integer.parseInt(args[0]);
			}

			// read in an image from a file
			img = ImageIO.read(new File("image.jpg"));

			// store the dimensions locally for convenience
			imgWidth  = img.getWidth();
			imgHeight = img.getHeight();

			// create an output image
			outputimage = new BufferedImage(imgWidth,imgHeight,BufferedImage.TYPE_INT_ARGB);

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

			// Create the Executor service that will manage the threads.
			ExecutorService executor = Executors.newFixedThreadPool(threads);

			// Set the x and y start positions to 0 for the first thread to 0.
			int startPosX = 0;
			int startPosY = 0;

			// Start the program timer.
			long startTime = System.currentTimeMillis();

			// Start each thread with their appropriate starting position. This depends on the number of threads and the
			// width and height of the image.
			for (int threadNumber=0; threadNumber<threads; threadNumber++) {

				// If the x starting position is outside the width of the image, reset it to 0 and move y down 1 row.
				if(startPosX >= imgWidth){
					startPosX = 0;
					startPosY++;
				}

				// If y is out of bounds of the height, then no additional thread shall be tasked with a computation as the image
				// has been completely covered.
				if(startPosY >= imgHeight)
					break;

				ConvolutionTask task = new ConvolutionTask(startPosX,startPosY);
				executor.execute(task);

				startPosX++;
			}

			// Wait for all threads ti be complete.
			executor.shutdown();
			while (!executor.isTerminated());

			// End the program timer.
			long endTime = System.currentTimeMillis();

			// Display the Time Elapsed to the user.
			System.out.println("Time Elapsed: "+(endTime-startTime));
			// ------------------------------------

			// Write out the image
			File outputfile = new File("outputimage.png");
			ImageIO.write(outputimage, "png", outputfile);

		} catch (Exception e) {
			System.out.println("ERROR " +e);
			e.printStackTrace();
		}
	}

	/**
	 * Runnable (thread) class that performs the convolution of an image with a given kernel
	 *
	 * */
	static class ConvolutionTask implements Runnable {

		// Position that the thread is currently at in the image.
		private Position position;

		public ConvolutionTask(int startX, int startY){
			this.position = new Position(startX,startY);
		}

		public void run(){

			// While there is a next position in the image for the thread, continue computations.
			boolean computePosition = true;
			while(computePosition){
				int x = this.position.getX();
				int y = this.position.getY();

				// Hold individual accumulators for each color channel.
				int redAcc = 0;
				int blueAcc = 0;
				int greenAcc = 0;

				// Run through the kernel and multiply each weight with the respective image pixel, and sum them up.
				for(int i = 0; i < kernel.length; i++){
					for(int j = 0; j < kernel[0].length; j++){
						if(x-1+j >= 0 && x-1+j < imgWidth && y-1+i >=0 && y-1+i < imgHeight){

							/* Get the pixel from the image that this thread is tasked to, and split them up into R, G
							and B by appropriately shifting their bits. Multiply each pixel value with the current
							kernel weight and add them to their respective accumulators.*/
							int p = img.getRGB(x-1+j,y-1+i);
							redAcc+=((p>>16)&0xff)*kernel[i][j];
							greenAcc+=((p>>8)&0xff)*kernel[i][j];
							blueAcc+=(p&0xff)*kernel[i][j];
						}
					}
				}

				// Contain each color channel value between 0 and 255
				redAcc = Math.max(Math.min(255, redAcc),0);
				blueAcc = Math.max(Math.min(255, blueAcc),0);
				greenAcc = Math.max(Math.min(255, greenAcc),0);

				// Put Alpha, R, G and B back together to form the output pixel value and store it in the output image.
				int argb = ((0xff)<<24) | (redAcc<<16) | (greenAcc<<8) | (blueAcc);
				outputimage.setRGB(x, y, argb);

				// Get the threads next tasked position.
				computePosition = nextPosition();
			}
		}

		/**
		 * Function that caculates the threads next position to perform computations on, and returns a boolean stating
		 * whether there is another position in the image.
		 *
		 * @return a boolean value stating whether the thread has another position in the image to perform calculations on.
		 * */
		public boolean nextPosition(){
			int curX = this.position.getX();
			int curY = this.position.getY();
			int nextX = 0;
			int nextY = 0;

			/* If the next x position, which is a distance equivalent to the number of threads tasked for the overall
			computation of the convolution, is out of bounds of the width of the image, and there is another row in
			the image, wrap the x position around the image and move down a row.*/
			if(curX+threads >= imgWidth){
				if(curY+1 < imgHeight){
					nextX = (curX+threads)%imgWidth;
					nextY = curY+1;
				}
				// If the there are no more rows in the image, the thread is done with its computations.
				else
					return false;
			}
			// The x position is still within the width of the image, so move to the next position on the same row
			else{
				nextX = curX+threads;
				nextY = curY;
			}

			// Set the new x and y positions of the thread and return true, as there is another position for computation.
			this.position.setX(nextX);
			this.position.setY(nextY);

			return true;
		}
	}

	/**
	 * Class that structures information regarding a threads x and y position
	 * */
	static class Position {
		private int x;
		private int y;

		public Position(int x, int y){
			this.x = x;
			this.y = y;
		}

		public int getX(){
			return this.x;
		}

		public void setX(int x){
			this.x = x;
		}

		public int getY(){
			return this.y;
		}

		public void setY(int y){
			this.y = y;
		}
	}

}
