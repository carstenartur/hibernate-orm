/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.tuple.GenerationTiming;

/**
 * Source-agnostic description of information needed to bind a singular attribute.
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeSource extends AttributeSource {
	/**
	 * Determine whether this is a virtual attribute or whether it physically exists on the users domain model.
	 *
	 * @return {@code true} indicates the attribute is virtual, meaning it does NOT exist on the domain model;
	 *         {@code false} indicates the attribute physically exists.
	 */
	boolean isVirtualAttribute();

	/**
	 * Obtain the nature of this attribute type.
	 *
	 * @return The attribute type nature
	 */
	SingularAttributeNature getSingularAttributeNature();

	/**
	 * Obtain a description of if/when the attribute value is generated by the database.
	 *
	 * @return The attribute value generation information
	 */
	GenerationTiming getGenerationTiming();

	/**
	 * Did the mapping specify that the given attribute value(s) should be inserted into the database?
	 *
	 * @return {@code true} indicates value(s) should be inserted; {@code false} indicates not.
	 */
	Boolean isInsertable();

	/**
	 * Did the mapping specify that the given attribute value(s) should be updated in the database?
	 *
	 * @return {@code true} indicates value(s) should be updated; {@code false} indicates not.
	 */
	Boolean isUpdatable();

	/**
	 * Should the attribute be  lazily loaded by bytecode enhancement?
	 *
	 * @return {@code true} to indicate the attribute should be lazily loaded by bytecode enhancement?
	 */
	boolean isBytecodeLazy();

	/**
	 * Retrieve the natural id mutability
	 *
	 * @return The mutability, see enum for meanings
	 */
	NaturalIdMutability getNaturalIdMutability();

}
