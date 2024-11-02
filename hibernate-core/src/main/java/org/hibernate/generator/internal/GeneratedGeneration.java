/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import org.hibernate.annotations.Generated;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.persister.entity.EntityPersister;

import java.util.EnumSet;

import static org.hibernate.generator.EventTypeSets.fromArray;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * A fairly generic {@link OnExecutionGenerator} which marks a property as generated in the
 * database with semantics given explicitly by a {@link Generated @Generated} annotation.
 *
 * @see Generated
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class GeneratedGeneration implements OnExecutionGenerator, BeforeExecutionGenerator {

	private final EnumSet<EventType> eventTypes;
	private final boolean writable;
	private final String[] sql;

	public GeneratedGeneration(EnumSet<EventType> eventTypes) {
		this.eventTypes = eventTypes;
		writable = false;
		sql = null;
	}

	public GeneratedGeneration(Generated annotation) {
		eventTypes = fromArray( annotation.event() );
		sql = isEmpty( annotation.sql() ) ? null : new String[] { annotation.sql() };
		writable = annotation.writable() || sql != null;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return eventTypes;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return writable;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return sql;
	}

	@Override
	public boolean writePropertyValue() {
		return writable && sql==null;
	}

	@Override
	public boolean generatedOnExecution() {
		return true;
	}

	@Override
	public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
		if ( !writable ) {
			return true;
		}

		// When this is the identifier generator and writable is true, allow pre-assigned identifiers
		final EntityPersister entityPersister = session.getEntityPersister( null, entity );
		return entityPersister.getGenerator() != this || entityPersister.getIdentifier( entity, session ) == null;
	}

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
		final EntityPersister entityPersister = session.getEntityPersister( null, owner );
		assert entityPersister.getGenerator() == this;
		return entityPersister.getIdentifier( owner, session );
	}

	@Override
	public boolean allowAssignedIdentifiers() {
		return writable;
	}
}
