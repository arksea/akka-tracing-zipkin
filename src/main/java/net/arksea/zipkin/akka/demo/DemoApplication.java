package net.arksea.zipkin.akka.demo;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

public class DemoApplication {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("system");
		ActorRef actor2 = system.actorOf(Actor2.props(), "actor2");
        ActorRef actor1 = system.actorOf(Actor1.props(actor2), "actor1");
		Thread.sleep(3000);
        actor1.tell(new Message3("init"), ActorRef.noSender());
        Thread.sleep(1000);
		actor1.tell(new Message1("start traceing"), ActorRef.noSender());
        Thread.sleep(5000);
        system.terminate();
        Thread.sleep(3000);
	}

}

