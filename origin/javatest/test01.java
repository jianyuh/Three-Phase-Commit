
public class test01 {
	public static void main(String[] args) {

		System.out.println("hello world");
		System.exit(0);
		try {
			Thread.sleep(10000);
		}catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	
	}
}
