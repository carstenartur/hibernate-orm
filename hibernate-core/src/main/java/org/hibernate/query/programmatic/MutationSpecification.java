/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.programmatic.internal.MutationSpecificationImpl;
import org.hibernate.query.restriction.Restriction;

/**
 * Specialization of {@link QuerySpecification} for programmatic customization of
 * {@linkplain MutationQuery mutation queries}.
 * <p>
 * The method {@link #addRestriction(Restriction)} allows application of additional
 * {@linkplain Restriction filtering} to the mutated entity. The static factory
 * methods of {@link Restriction} are used to express filtering criteria of various
 * kinds.
 * <p>
 * Once all {@linkplain #addRestriction restrictions} are specified, call
 * {@link #createQuery createQuery()} to obtain an {@linkplain SelectionQuery
 * executable mutation query object}.
 *
 * @param <T> The entity type which is the target of the mutation.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface MutationSpecification<T> extends QuerySpecification<T> {

	/**
	 * Covariant override.
	 */
	@Override
	MutationSpecification<T> addRestriction(Restriction<T> restriction);

	/**
	 * A function capable of modifying or augmenting a criteria query.
	 *
	 * @param <T> The target entity type
	 */
	@FunctionalInterface
	interface Augmentation<T> {
		void augment(CriteriaBuilder builder, CommonAbstractCriteria query, Root<T> mutationTarget);
	}

	/**
	 * Add an {@linkplain Augmentation augmentation} to the specification.
	 *
	 * @param augmentation A function capable of modifying or augmenting a criteria query.
	 *
	 * @return {@code this} for method chaining.
	 */
	MutationSpecification<T> addAugmentation(Augmentation<T> augmentation);

	/**
	 * Finalize the building and create the {@linkplain SelectionQuery} instance.
	 */
	@Override
	MutationQuery createQuery(SharedSessionContract session);

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain MutationQuery} based on a base HQL statement,
	 * allowing the addition of {@linkplain MutationSpecification#addRestriction restrictions}.
	 *
	 * @param hql The base HQL query (expected to be an {@code update} or {@code delete} query).
	 * @param mutationTarget The entity which is the target of the mutation.
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 * {@code mutationTarget} and {@code <T>} are both expected to refer to the mutation target.
	 *
	 * @throws IllegalMutationQueryException Only {@code update} and {@code delete} are supported;
	 * this method will throw an exception if the given HQL query is not an {@code update} or {@code delete}.
	 */
	static <T> MutationSpecification<T> create(Class<T> mutationTarget, String hql) {
		return new MutationSpecificationImpl<>( hql, mutationTarget );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain MutationQuery} based on the given criteria update,
	 * allowing the addition of {@linkplain MutationSpecification#addRestriction restrictions}.
	 *
	 * @param criteriaUpdate The criteria update query
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	static <T> MutationSpecification<T> create(CriteriaUpdate<T> criteriaUpdate) {
		return new MutationSpecificationImpl<>( criteriaUpdate );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain MutationQuery} based on the given criteria delete,
	 * allowing the addition of {@linkplain MutationSpecification#addRestriction restrictions}.
	 *
	 * @param criteriaDelete The criteria delete query
	 *
	 * @param <T> The root entity type for the mutation (the "target").
	 */
	static <T> MutationSpecification<T> create(CriteriaDelete<T> criteriaDelete) {
		return new MutationSpecificationImpl<>( criteriaDelete );
	}
}
