/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedQueryMemento;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common attributes shared across the mapping of named HQL, native
 * and "callable" queries defined in annotations, orm.xml and hbm.xml
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedQueryDefinition<E> {
	/**
	 * The name under which the query is to be registered
	 */
	String getRegistrationName();

	/**
	 * The expected result type of the query, or {@code null}.
	 */
	@Nullable
	Class<E> getResultType();

	/**
	 * Resolve the mapping definition into its run-time memento form
	 */
	NamedQueryMemento<E> resolve(SessionFactoryImplementor factory);
}
