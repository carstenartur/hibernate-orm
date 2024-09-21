/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.EntityNameResolver;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;

/**
 * @author Steve Ebersole
 */
public interface MappingMetamodelImplementor extends MappingMetamodel, QueryParameterBindingTypeResolver {

	/**
	 * Retrieves a set of all the collection roles in which the given entity is a participant, as either an
	 * index or an element.
	 *
	 * @param entityName The entity name for which to get the collection roles.
	 *
	 * @return set of all the collection roles in which the given entityName participates.
	 */
	Set<String> getCollectionRolesByEntityParticipant(String entityName);

	/**
	 * Access to the EntityNameResolver instance that Hibernate is configured to
	 * use for determining the entity descriptor from an instance of an entity
	 */
	Collection<EntityNameResolver> getEntityNameResolvers();

}
