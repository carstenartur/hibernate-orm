/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Brett Meyer
 */
@Entity
public class Location {
	@Id @GeneratedValue
	public long id;

	public String address;

	public int zip;

	public int getZip() {
		return zip;
	}
}
