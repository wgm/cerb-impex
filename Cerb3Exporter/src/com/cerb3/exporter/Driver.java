package com.cerb3.exporter;

import com.cerb3.exporter.entities.Ticket;
import com.cerb3.exporter.entities.Worker;

public class Driver {
	public Driver() {
		new Worker().export();
//		new Address().export();
		new Ticket().export();
	}
}
