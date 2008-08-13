package com.cerberusweb.cerb2;

import com.cerberusweb.cerb2.entities.Ticket;
import com.cerberusweb.cerb2.entities.Worker;

public class Driver {
	public Driver() {
		new Worker().export();
//		new Address().export();
		new Ticket().export();
	}
}
