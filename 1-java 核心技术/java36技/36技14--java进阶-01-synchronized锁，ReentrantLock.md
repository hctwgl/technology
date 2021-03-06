# Lesson14 | synchronized 和 ReentrantLock

---

### 多线程并发安全的核心知识点
	理解什么是线程安全
	synchronized、ReentrantLock 等机制的基本使用与案例
	掌握 synchronized、ReentrantLock 底层实现；
	理解锁膨胀、降级；理解偏斜锁、自旋锁、轻量级锁、重量级锁等概念。
	掌握并发包中 java.util.concurrent.lock 各种不同实现和案例分析。


##### 理解什么是线程安全
	线程安全是一个多线程环境下正确性的概念，
	也就是保证多线程环境下共享的、可修改的状态的正确性，这里的状态反映在程序中其实可以看作是数据。

	换个角度来看，如果状态不是共享的，或者不是可修改的，也就不存在线程安全问题，
	进而可以推理出保证线程安全的两个办法：
		1、封装：通过封装，我们可以将对象内部状态隐藏、保护起来。
		2、不可变：final 和 immutable 保证变量不可变，因此是只读的，不存在线程并发写的问题。

	线程安全需要保证几个基本特性：
		1、原子性，简单说就是相关操作不会中途被其他线程干扰，一般通过同步机制实现。
		2、可见性，是一个线程修改了某个共享变量，其状态能够立即被其他线程知晓，
		通常被解释为将线程本地状态反映到主内存上，volatile 就是负责保证可见性的。
		3、有序性，是保证线程内串行语义，避免指令重排等。

## Java中提供了哪些锁？
	隐式锁：
		synchronized	最经典的锁实现，随着JVM版本升级不断进行着优化；
	
	显示锁：	
		ReentrantLock	在实现synchronized所有同步功能的基础上，进一步实现锁的细粒度控制；
		ReentrantReadWriteLock 	读写锁；
		StampedLock		乐观读写锁，对ReentrantReadWriteLock进一步优化；


## synchronized
	synchronized 是 Java 内建的同步机制，所以也有人称其为 Intrinsic Locking，
	它提供了互斥的语义和可见性，当一个线程已经获取当前锁时，其他试图获取的线程只能等待或者阻塞在那里。
	
	在 Java 5 以前，synchronized 是仅有的同步手段，
	在代码中， synchronized 可以用来修饰方法，也可以使用在特定的代码块儿上，
	本质上 synchronized 方法等同于把方法全部语句用 synchronized 块包起来。

## ReentrantLock
	ReentrantLock，通常翻译为再入锁，是 Java 5 提供的锁实现，它的语义和 synchronized 基本相同。
	再入锁通过代码直接调用 lock() 方法获取，代码书写也更加灵活。
	与此同时，ReentrantLock 提供了很多实用的方法，能够实现很多 synchronized 无法做到的细节控制，
		比如可以控制 fairness，也就是公平性，或者利用定义条件等。
	> 但是，编码中也需要注意，必须要明确调用 unlock() 方法释放，不然就会一直持有该锁。

## ReentrantLock的特性
	1、什么是锁再入？
	它是表示当一个线程试图获取一个它已经获取的锁时，这个获取动作就自动成功，这是对锁获取粒度的一个概念，
	也就是锁的持有是以线程为单位而不是基于调用次数。	
	Java 锁实现强调再入性是为了和 pthread 的行为进行区分。

	2、再入锁可以设置公平性（fairness），我们可在创建再入锁时选择是否是公平的。
		ReentrantLock fairLock = new ReentrantLock(true);
	这里所谓的公平性是指在竞争场景中，当公平性为真时，会倾向于将锁赋予等待时间最久的线程。
	公平性是减少线程“饥饿”（个别线程长期等待锁，但始终无法获取）情况发生的一个办法。

	如果使用 synchronized，我们根本无法进行公平性的选择，其永远是不公平的，
	这也是主流操作系统线程调度的选择。
	通用场景中，公平性未必有想象中的那么重要，Java 默认的调度策略很少会导致 “饥饿”发生。
	与此同时，若要保证公平性则会引入额外开销，自然会导致一定的吞吐量下降。
	所以，我建议只有当你的程序确实有公平性需要的时候，才有必要指定它。	

	3、我们再从日常编码的角度学习下再入锁。
	为保证锁释放，每一个 lock() 动作，我建议都立即对应一个 try-catch-finally，
	典型的代码结构如下，这是个良好的习惯。
		ReentrantLock fairLock = new ReentrantLock(true);
		try {
		    // do something
		} finally {
		     fairLock.unlock(); // 一定要确保锁被释放
		}

	4、ReentrantLock 相比 synchronized，因为可以像普通对象一样使用，
	所以可以利用其提供的各种便利方法，进行“精细的同步操作”，甚至是实现 synchronized 难以表达的用例，如：
		带超时的获取锁尝试。
		可以判断是否有线程，或者某个特定线程，在排队等待获取锁。
		可以响应中断请求（synchronized实现的同步，等待锁的线程不能响应中断请求）。
	
	5、条件变量（java.util.concurrent.Condition）
	如果说 ReentrantLock 是 synchronized 的替代选择，
	Condition 则是将 wait、notify、notifyAll 等操作转化为相应的对象，
	将复杂而晦涩的同步操作转变为直观可控的对象行为。
	
	条件变量最为典型的应用场景就是标准类库中的 ArrayBlockingQueue 等。
	
	> 参考下面的源码，首先，通过再入锁获取条件变量：
		/** Condition for waiting takes */
		private final Condition notEmpty;  // 条件1
		
		/** Condition for waiting puts */
		private final Condition notFull; // 条件2
		
		public ArrayBlockingQueue(int capacity, boolean fair) {
		    if (capacity <= 0)
		        throw new IllegalArgumentException();
		    this.items = new Object[capacity];
		    lock = new ReentrantLock(fair);
		    notEmpty = lock.newCondition(); // 同一个lock上创建条件对象
		    notFull =  lock.newCondition(); // 同一个lock上创建条件对象
		}

	两个条件变量是从同一再入锁创建出来，然后使用在特定操作中，
	如下面的 take 方法，判断和等待条件满足：
		public E take() throws InterruptedException {
		    final ReentrantLock lock = this.lock;
		    lock.lockInterruptibly();
		    try {
		        while (count == 0)
		            notEmpty.await(); // 非空条件不满足，因此阻塞等待
		        return dequeue();
		    } finally {
		        lock.unlock();
		    }
		}

	当队列为空时，试图 take 的线程的正确行为应该是等待入队发生，而不是直接返回，
	这是 BlockingQueue 的语义，使用条件 notEmpty 就可以优雅地实现这一逻辑。

	那么，怎么保证入队触发后续 take 操作呢？
	请看 enqueue 实现：
		private void enqueue(E e) {
		    // assert lock.isHeldByCurrentThread();
		    // assert lock.getHoldCount() == 1;
		    // assert items[putIndex] == null;
		    final Object[] items = this.items;
		    items[putIndex] = e;
		    if (++putIndex == items.length) putIndex = 0;
		    count++;
		    notEmpty.signal(); // 通知等待的线程，非空条件已经满足
		}

	通过 signal/await 的组合，完成了条件判断和通知等待线程，非常顺畅就完成了状态流转。
	注意，signal 和 await 成对调用非常重要，不然假设只有 await 动作，线程会一直等待直到被打断（interrupt）。


## java.util.concurrent.lock 提供的其他锁实现
	Java 可不是只有 ReentrantLock 一种显式的锁类型，还有其他类型可以供你选用。
	1、读写锁 	java.util.concurrent.locks.ReentrantReadWriteLock
	2、优化读写锁 java.util.concurrent.locks.StampedLock

![](img/reentrantlock.png)

## 为什么我们需要读写锁（ReadWriteLock）等其他锁呢？
	虽然 ReentrantLock 和 synchronized 简单实用，但是行为上有一定局限性，
	通俗点说就是“太霸道”，要么不占，要么独占。
	实际应用场景中，有的时候不需要大量竞争的写操作，而是以并发读取为主，
	如何进一步优化并发操作的粒度呢？	

	Java 并发包提供的读写锁等扩展了锁的能力，
	它所基于的原理是多个读操作是不需要互斥的，因为读操作并不会更改数据，所以不存在互相干扰。
	而写操作则会导致并发一致性的问题，所以写线程之间、读写线程之间，需要精心设计的互斥逻辑。
	
## ReentrantReadWriteLock 读写锁	
	在运行过程中，如果读锁试图锁定时，写锁是被某个线程持有，读锁将无法获得，只好等待对方操作结束，
	这样就可以自动保证不会读取到有争议的数据。

	读写互斥、写写互斥、读读可安全并发。
	
	> 适用场景：
		基于读写锁实现的数据结构，当数据量较大，并发读多、并发写少的时候，能够比纯同步版本凸显出优势。

	public class RWSample {
	    private final Map<String, String> m = new TreeMap<>(); // 线程不安全的Map
	    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	    private final Lock r = rwl.readLock();
	    private final Lock w = rwl.writeLock();
		
		// 使用读锁
	    public String get(String key) {
	        r.lock();
	        System.out.println(" 读锁锁定！");
	        try {
	            return m.get(key);
	        } finally {
	            r.unlock();
	        }
	    }
		
		// 使用写锁
	    public String put(String key, String entry) {
	        w.lock();
	    	System.out.println(" 写锁锁定！");
	            try {
	                return m.put(key, entry);
	            } finally {
	                w.unlock();
	            }
	        }
	    // …
	}

#### StampedLock 乐观读写锁

	读写锁看起来比 synchronized 的粒度似乎细一些，
	但在实际应用中，其表现也并不尽如人意，主要还是因为相对比较大的开销。
	
	所以，JDK 在后期引入了 StampedLock，在提供类似读写锁的同时，还支持"优化读模式"。
	优化读基于假设，大多数情况下读操作并不会和写操作冲突，
	其逻辑是先试着修改，然后通过 validate 方法确认是否进入了写模式，
		如果没有进入，就成功避免了开销；
		如果进入，则尝试获取读锁。

	> StampedLock 竟然也是个单独的类型，从类图结构可以看出它是不支持再入性的语义的，
	也就是它不是以持有锁的线程为单位。

	public class StampedSample {
	    private final StampedLock sl = new StampedLock();
	
	    void mutate() {
	        long stamp = sl.writeLock(); // 写操作，直接上锁
	        try {
	            write();
	        } finally {
	            sl.unlockWrite(stamp);
	        }
	    }
	
	    Data access() {
	        long stamp = sl.tryOptimisticRead(); // 读操作，先获取戳记
	        Data data = read();
	        if (!sl.validate(stamp)) { // 如果有写操作发生，才需要获取读锁
	            stamp = sl.readLock();
	            try {
	                data = read();
	            } finally {
	                sl.unlockRead(stamp);
	            }
	        }
	        return data;
	    }
	    // …
	}


###### 问答1： synchronized 底层如何实现？

	synchronized 代码块是由一对儿 monitorenter/monitorexit 指令实现的，Monitor 对象是同步的基本实现单元。

	在 Java 6 之前，Monitor 的实现完全是依靠操作系统内部的互斥锁，
	因为需要进行用户态到内核态的切换，所以同步操作是一个无差别的重量级操作。

	现代的（Oracle）JDK 中，JVM 对此进行了大刀阔斧地改进，提供了三种不同的 Monitor 实现，
	也就是常说的三种不同的锁：
		偏斜锁（Biased Locking）、轻量级锁和重量级锁，大大改进了其性能。
	
	> 当没有竞争出现时，默认会使用偏斜锁。
	JVM 会利用 CAS 操作（compare and swap），在对象头上的 Mark Word 部分设置线程 ID，
	以表示这个对象偏向于当前线程，所以并不涉及真正的互斥锁。
	这样做的假设是基于在很多应用场景中，大部分对象生命周期中最多会被一个线程锁定，使用偏斜锁可以降低无竞争开销。

	> 如果有另外的线程试图锁定某个已经被偏斜过的对象，JVM 就需要撤销（revoke）偏斜锁，并切换到轻量级锁实现。
	
	>轻量级锁依赖 CAS 操作 Mark Word 来试图获取锁，如果重试成功，就使用普通的轻量级锁；
	否则，进一步升级为重量级锁。

###### 问答2： 什么是锁的升级、降级？

	所谓锁的升级、降级，就是 JVM 优化 synchronized 运行的机制，
	当 JVM 检测到不同的竞争状况时，会自动切换到适合的锁实现，这种切换就是锁的升级、降级。

###### 问答3： 什么是自旋锁？
	自旋锁:
	竞争锁的失败的线程，并不会真实的在操作系统层面挂起等待，
	而是JVM会让线程做几个空循环(基于预测在不久的将来就能获得)，
	在经过若干次循环后，如果可以获得锁，那么进入临界区，
	如果还不能获得锁，才会真实的将线程在操作系统层面进行挂起。

	适用场景:
	自旋锁可以减少线程的阻塞，这对于锁竞争不激烈，且占用锁时间非常短的代码块来说，有较大的性能提升，
	因为自旋的消耗会小于线程阻塞挂起操作的消耗。

	> 切记，自旋锁只有在多核CPU上有效果，单核毫无效果，只是浪费时间。

---
## 知识扩展

### 1、有人说 synchronized 最慢，这话靠谱吗？
	synchronized 和 ReentrantLock 的性能不能一概而论，
	早期版本 synchronized 在很多场景下性能相差较大，
	> 在后续版本进行了较多改进，在低竞争场景中表现可能优于 ReentrantLock。
	
	底层使用JIT对synchronized进行的优化？
		This can mean the JIT can optimise synchronised blocks.

	从性能角度，synchronized 早期的实现比较低效，对比 ReentrantLock，大多数场景性能都相差较大。
	但是在 Java 6 中对其进行了非常多的改进
	> 可以参考性能对比：
		https://dzone.com/articles/synchronized-vs-lock
	
	不过，在高竞争情况下，ReentrantLock 仍然有一定优势。

	在大多数情况下，无需纠结于性能，还是考虑代码“书写结构的便利性、可维护性”等。
	
	注意：
		JDK8中ConcurrentHashMap内部的put方法就使用了synchronized进行同步控制。


### 2、synchronized 底层如何实现？
	首先，synchronized 的行为是 JVM runtime 的一部分，所以我们需要先找到 Runtime 相关的功能实现。
	通过在代码中查询类似“monitor_enter”或“Monitor Enter”，
	很直观的就可以定位到sharedRuntime.cpp/hpp，它是解释器和编译器运行时的基类。：
### [sharedRuntime.cpp](http://hg.openjdk.java.net/jdk/jdk/file/6659a8f57d78/src/hotspot/share/runtime/sharedRuntime.cpp)

在 sharedRuntime.cpp 中，下面代码体现了 synchronized 的主要逻辑。

	Handle h_obj(THREAD, obj);
	  // UseBiasedLocking 是一个检查，因为，在 JVM 启动时，我们可以指定是否开启偏斜锁。
	  if (UseBiasedLocking) {
	    // Retry fast entry if bias is revoked to avoid unnecessary inflation
	    ObjectSynchronizer::fast_enter(h_obj, lock, true, CHECK);
	  } else {
		// 没有开启偏斜锁，则直接进入轻量级锁的获取
	    ObjectSynchronizer::slow_enter(h_obj, lock, CHECK);
	  }

	偏斜锁并不适合所有应用场景，撤销操作（revoke）是比较重的行为，
	只有当存在较多不会真正竞争的 synchronized 块儿时，才能体现出明显改善。
	实践中对于偏斜锁的一直是有争议的，有人甚至认为，当你需要大量使用并发类库时，往往意味着你不需要偏斜锁。
	从具体选择来看，我还是建议需要在实践中进行测试，根据结果再决定是否使用。

	还有一方面是，偏斜锁会延缓 JIT 预热的进程，所以很多性能测试中会显式地关闭偏斜锁，命令如下：
		-XX:-UseBiasedLocking

	fast_enter 是我们熟悉的完整锁获取路径，方法内部若获取偏斜锁失败，则切换到slow_enter；
	slow_enter 则是绕过偏斜锁，直接进入轻量级锁获取逻辑；

#
	synchronized 代码块是由一对 monitorenter/monitorexit 指令实现的，Monitor 对象是同步的基本实现。
	
	synchronized 是 JVM 内部的 Intrinsic(固有) Lock，
	所以偏斜锁、轻量级锁、重量级锁的代码实现，并不在核心类库部分，而是在 JVM 的代码中。

	synchronized (this) {
	    int former = sharedState ++;
	    int latter = sharedState;
	    // …
	}

	如果用 javap 反编译，可以看到类似片段，利用 monitorenter/monitorexit 对实现了同步的语义：
		11: astore_1
		12: monitorenter // 获取监视器对象
		13: aload_0
		14: dup
		15: getfield      #2                  // Field sharedState:I
		18: dup_x1
		…
		56: monitorexit // 释放监视器对象
	

## [biasedLocking](http://hg.openjdk.java.net/jdk/jdk/file/6659a8f57d78/src/hotspot/share/runtime/biasedLocking.cpp)
	明白它是通过 CAS 设置 Mark Word，对象头中 Mark Word 的结构，可以参考下图：
![](img/obj-header.png)	


### 3、[AQS框架](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/locks/AbstractQueuedSynchronizer.html) - 阻塞框架基础类
	Java 并发包内的各种同步工具，不仅仅是各种 Lock，其他的如Semaphore，CountDownLatch等，
	都是基于一种AQS框架实现的。

	Provides a framework for implementing blocking locks and related synchronizers (semaphores, events, etc) 
	that rely on first-in-first-out (FIFO) wait queues. 

	This class is designed to be a useful basis for most kinds of synchronizers 
	that rely on a single atomic int value to represent state. 