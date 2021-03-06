package thread02.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
/**
 * t2 线程不再主动轮询结果，改为等待通知
 * 
 * 实时通知另一个线程-CountDownLatch
 */
public class TestListAdd3 {
	private volatile static List<String> list = new ArrayList<>();	
	
	public void add(){
		list.add("string");
	}
	public int size(){
		return list.size();
	}
	
	public static void main(String[] args) {
		
		final TestListAdd3 list2 = new TestListAdd3();
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("t1启动..");
					for(int i = 0; i <10; i++){
						list2.add();
						System.out.println("当前线程：" + Thread.currentThread().getName() + "添加了一个元素..");
						Thread.sleep(500);
						if(list2.size() == 5){
							latch.countDown();
							System.out.println("已经发出通知..t1 notify t2");
						}
					}						
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}, "t1");
		
		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("t2启动..");
				if(list2.size() != 5){
					try {
						System.out.println("t2 wait..");
						latch.await();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// CountDownLatch的count==0时，当前线程可立即向下执行
				System.out.println("当前线程：" + Thread.currentThread().getName() + "收到通知线程停止..");
				throw new RuntimeException();
			}
		}, "t2");
		
		t2.start();
		
		t1.start();
		
	}
	
}
