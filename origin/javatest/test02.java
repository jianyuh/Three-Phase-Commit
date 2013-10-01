
public class test02 {
	public static void main(String[] args) {
		try {
			Thread.sleep(10000);
		}catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		System.out.println("hello world");
		System.exit(0);
	}
}
