package com.sri.ai.distributed.sgdpllt.actor;

import java.util.Random;

import com.sri.ai.distributed.sgdpllt.util.AkkaUtil;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class LocalActorTest {	
	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("localactors");
		
		ActorRef rootCompute = system.actorOf(Props.create(LocalComputeActor.class));
		Future<Object> futureResult = Patterns.ask(rootCompute, 0, AkkaUtil.getDefaultTimeout());
		Integer result = (Integer) Await.result(futureResult, AkkaUtil.getDefaultTimeout().duration());
		System.out.println("Final result="+result);
		system.terminate();
	}
	
	public static class LocalComputeActor extends UntypedActor {
		LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
		@Override
		public void onReceive(Object message) throws Exception {
			if (message instanceof Integer) {
				log.debug("compute value={}", message);
				Integer value = (Integer) message;
				Integer result;
				
				// Simulate a 0 to 4 second computation
				long start    = System.currentTimeMillis();
				long spinTill = start + new Random().nextInt(2000);
				while (System.currentTimeMillis() < spinTill) {
					// Keep spinning.
				}
				
				if (value == 8) {
					result = value;
				}
				else {
					ActorRef subCompute1 = getContext().actorOf(Props.create(LocalComputeActor.class));
					ActorRef subCompute2 = getContext().actorOf(Props.create(LocalComputeActor.class));
					
					Future<Object> futureResult1 = Patterns.ask(subCompute1, value+1, AkkaUtil.getDefaultTimeout());
					Future<Object> futureResult2 = Patterns.ask(subCompute2, value+1, AkkaUtil.getDefaultTimeout());
					
					Integer subResult1 = (Integer) Await.result(futureResult1, AkkaUtil.getDefaultTimeout().duration());
					Integer subResult2 = (Integer) Await.result(futureResult2, AkkaUtil.getDefaultTimeout().duration());
					
					result = subResult1+subResult2;					
				}
				
				getSender().tell(result, getSelf());
				
				log.debug("compute result={} compute time={}ms.", result, spinTill-start);
				
				getContext().stop(getSelf());
			}
			else {
				unhandled(message);
			}
		}
	}
}
