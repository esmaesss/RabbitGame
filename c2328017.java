package RabbitCarrot;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class c2328017{




    static int BOX_COUNT, RABBIT_COUNT;
    static int X, Y, Z;

    static Box[] boxes;
    static List<Rabbit> rabbits = new ArrayList<>();

    static AtomicBoolean gameOver = new AtomicBoolean(false);
    static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter number of rabbits: ");
        RABBIT_COUNT = sc.nextInt();

        System.out.print("Enter number of boxes: ");
        BOX_COUNT = sc.nextInt();

        System.out.print("Enter carrot producing rate (X): ");
        X = sc.nextInt();

        System.out.print("Enter carrot timeout (Y): ");
        Y = sc.nextInt();

        System.out.print("Enter rabbit sleeping time (Z): ");
        Z = sc.nextInt();

        System.out.println("The game starts");

        boxes = new Box[BOX_COUNT];
        for (int i = 0; i < BOX_COUNT; i++) {
            boxes[i] = new Box(i);
        }

        String[] names = {"Remzi", "Canan", "Neca", "Fatma", "Azra"};

        Thread[] rabbitThreads = new Thread[RABBIT_COUNT];
        for (int i = 0; i < RABBIT_COUNT; i++) {
            Rabbit r = new Rabbit(names[i % names.length]);
            rabbits.add(r);
            rabbitThreads[i] = new Thread(r);
            rabbitThreads[i].start();
        }

        Thread personThread = new Thread(new Person());
        personThread.start();

        for (Thread t : rabbitThreads) t.join();

        gameOver.set(true);
        personThread.interrupt();
        scheduler.shutdownNow();

        Rabbit winner = rabbits.stream()
                .max(Comparator.comparingInt(r -> r.score))
                .orElse(null);

        System.out.println("Winner: " + winner.name + " with " + winner.score + " points");
        System.out.println("Game over!");
    }

  
    static class Box {
        int id;
        Carrot carrot;

        Box(int id) { this.id = id; }
    }

   
    static class Carrot {
        long id;
        long createdAt;

        Carrot(long id) {
            this.id = id;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt >= Y;
        }
    }

   
    static class Rabbit implements Runnable {
        String name;
        volatile int position = 0;
        int score = 0;

        Rabbit(String name) {
            this.name = name;
        }

        public void run() {
            while (position < BOX_COUNT - 1) {
                try { Thread.sleep(Z); }
                catch (InterruptedException e) { return; }

                position++;
                Box box = boxes[position];

                synchronized (box) {
                    System.out.println(name + " jumps to box " + position);

                    if (box.carrot != null && !box.carrot.isExpired()) {
                        score++;
                        box.carrot = null;
                        System.out.println(name + " eats carrot in box " + position);
                    }
                }
            }
            System.out.println(name + " has " + score + " points");
        }
    }


    static class Person implements Runnable {
        Random rand = new Random();

        public void run() {
            while (!gameOver.get()) {
                try { Thread.sleep(X); }
                catch (InterruptedException e) { return; }

                int minPos = rabbits.stream()
                        .mapToInt(r -> r.position)
                        .min()
                        .orElse(0);

                int boxId = rand.nextInt(BOX_COUNT - minPos) + minPos;
                Box box = boxes[boxId];

                synchronized (box) {
                    if (box.carrot == null) {
                        Carrot c = new Carrot(System.nanoTime());
                        box.carrot = c;

                        System.out.println("Person puts carrot to box " + boxId);

                        scheduler.schedule(() -> {
                            synchronized (box) {
                                if (box.carrot == c && c.isExpired()) {
                                    box.carrot = null;
                                    System.out.println("Carrot in box " + boxId + " removed");
                                }
                            }
                        }, Y, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
}

