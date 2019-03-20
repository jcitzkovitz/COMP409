package A3;

public class q2 {
	public static void main(String[] args){
		if(args.length != 4){
			System.out.println("You must pass the following arguments: t (integer - # threads), q (floating point - probability of deleting), "
					+ "d (integer - time to sleep between operations) and n (integer - number of iterations)");
			System.exit(0);
		}
		
		long runTime = Questions.runQuestions(args[0], args[1], args[2], args[3], true, true);
		
		System.out.println(args[0] + " threads run time: " + runTime);
	}
}
