package com.sri.ai.distributed.sgdpllt.actor;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;

public class LocalActorTest {
	private static final Timeout _defaultTimeout = new Timeout(3600, TimeUnit.SECONDS);
	
	public static void main(String[] args) throws Exception {
		ActorSystem system = ActorSystem.create("localactors");
		
		ActorRef rootCompute = system.actorOf(Props.create(LocalComputeActor.class));
		Future<Object> futureResult = Patterns.ask(rootCompute, 0, _defaultTimeout);
		Integer result = (Integer) Await.result(futureResult, _defaultTimeout.duration());
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
				long spinTill = System.currentTimeMillis() + new Random().nextInt(4000);
				while (System.currentTimeMillis() < spinTill) {
					// Keep spinning.
				}
				
				if (value == 8) {
					result = value;
				}
				else {
					ActorRef subCompute1 = getContext().actorOf(Props.create(LocalComputeActor.class));
					ActorRef subCompute2 = getContext().actorOf(Props.create(LocalComputeActor.class));
					
					Future<Object> futureResult1 = Patterns.ask(subCompute1, value+1, _defaultTimeout);
					Future<Object> futureResult2 = Patterns.ask(subCompute2, value+1, _defaultTimeout);
					
					Integer subResult1 = (Integer) Await.result(futureResult1, _defaultTimeout.duration());
					Integer subResult2 = (Integer) Await.result(futureResult2, _defaultTimeout.duration());
					
					result = subResult1+subResult2;					
				}
				
				getSender().tell(result, getSelf());
				
				log.debug("compute result={}", result);
				
				getContext().stop(getSelf());
			}
			else {
				unhandled(message);
			}
		}
	}
}
