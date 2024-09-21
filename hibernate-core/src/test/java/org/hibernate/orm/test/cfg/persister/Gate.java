/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.persister;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Gate {
	@Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;
}
