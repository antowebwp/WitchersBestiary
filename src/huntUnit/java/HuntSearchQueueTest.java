import com.witchersbestiary.contracts.hunt.HuntSearchQueue;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Deterministic scheduler checks, not a Minecraft performance benchmark. */
public final class HuntSearchQueueTest {
    static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        for (int count : new int[]{1,5,10}) {
            var time = new AtomicLong(); var queue = new HuntSearchQueue(time::get,1_000_000,64);
            var probes = new int[count]; var owners = new UUID[count];
            for (int i=0;i<count;i++) {
                int index=i; owners[i]=UUID.randomUUID();
                var job=new HuntSearchQueue.Search(){ public UUID owner(){return owners[index];}
                    public HuntSearchQueue.Outcome step(){probes[index]++;time.addAndGet(200_000);return probes[index]==3?HuntSearchQueue.Outcome.WAITING:HuntSearchQueue.Outcome.CONTINUE;}};
                check(queue.enqueue(job)); check(!queue.enqueue(job));
            }
            int ticks=0; while(queue.pending()>0){var sample=queue.tick();check(sample.nanos()<=1_000_000);check(sample.probes()<=5);check(++ticks<20);}
            for(int n:probes)check(n==3);
            System.out.println("PASS synthetic fairness/budget/waiting for "+count+" jobs in "+ticks+" ticks");
        }
        var queue=new HuntSearchQueue(()->0L,1_000_000,2);var id=UUID.randomUUID();
        check(queue.enqueue(new HuntSearchQueue.Search(){public UUID owner(){return id;}public HuntSearchQueue.Outcome step(){return HuntSearchQueue.Outcome.CONTINUE;}}));
        check(queue.tick().probes()==2);check(queue.cancel(id));check(queue.pending()==0);check(!queue.cancel(id));
        check(queue.enqueue(new HuntSearchQueue.Search(){public UUID owner(){return id;}public HuntSearchQueue.Outcome step(){return HuntSearchQueue.Outcome.FOUND;}}));
        check(queue.tick().pending()==0);queue.clear();check(queue.pending()==0);
        System.out.println("PASS cancellation, fixed step cap, completion and re-enqueue");
    }
}
