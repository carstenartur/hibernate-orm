/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.programmatic.internal.SelectionSpecificationImpl;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

/**
 * Specialization of {@link QuerySpecification} for programmatic customization of
 * {@linkplain SelectionQuery selection queries} with ordering and restriction criteria.
 * <ul>
 * <li>The method {@link #addRestriction(Restriction)} allows application of additional
 *     {@linkplain Restriction filtering} to the query results. The static factory methods
 *     of {@link Restriction} are used to express filtering criteria of various kinds.
 * <li>Refinement or replacement of the query sorting criteria is possible via the methods
 *     {@link #addOrdering(Order)} and {@link #setOrdering(List)}, together with the static
 *     factory methods of {@link Order}.
 * </ul>
 * <p>
 * Once all {@linkplain #addOrdering sorting} and {@linkplain #addRestriction restrictions}
 * are specified, call {@link #createQuery createQuery()} to obtain an
 * {@linkplain SelectionQuery executable selection query object}.
 * <pre>
 * SelectionSpecification.create(Book.class, "from Book where discontinued = false")
 *         .addRestriction(Restriction.contains(Book_.title, "hibernate", false))
 *         .setOrdering(Order.desc(Book_.title))
 *         .createQuery(session)                       // obtain a SelectionQuery
 *         .setPage(Page.first(50))
 *         .getResultList();
 * </pre>
 * <p>
 * A {@code SelectionSpecification} always represents a query which returns a singe root
 * entity. The restriction and ordering criteria are interpreted as applying to the field
 * and properties of this root entity.
 *
 * @param <T> The entity type returned by the query
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface SelectionSpecification<T> extends QuerySpecification<T> {
	/**
	 * Adds an ordering to the selection specification.
	 * Appended to any previous ordering.
	 *
	 * @param order The ordering fragment to be added.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> addOrdering(Order<T> order);

	/**
	 * Sets the ordering for this selection specification.
	 * If ordering was already defined, this method drops the previous ordering
	 * in favor of the passed {@code orders}.
	 *
	 * @param order The ordering fragment to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> setOrdering(Order<T> order);

	/**
	 * Sets the sorting for this selection specification.
	 * If sorting was already defined, this method drops the previous sorting
	 * in favor of the passed {@code orders}.
	 *
	 * @param orders The sorting fragments to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> setOrdering(List<Order<T>> orders);

	/**
	 * Covariant override.
	 */
	@Override
	SelectionSpecification<T> addRestriction(Restriction<T> restriction);

	/**
	 * Add a fetch {@linkplain Path path} to the specification.
	 *
	 * @param fetchPath The path to fetch
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> addFetching(Path<T,?> fetchPath);

	/**
	 * A function capable of modifying or augmenting a criteria query.
	 *
	 * @param <T> The root entity type
	 */
	@FunctionalInterface
	interface Augmentation<T> {
		void augment(CriteriaBuilder builder, CriteriaQuery<T> query, Root<T> root);
	}

	/**
	 * Add an {@linkplain Augmentation augmentation} to the specification.
	 * <p>
	 * For example:
	 * <pre>
	 * SelectionSpecification.create(Book.class)
	 *       .addAugmentation((builder, query, book) ->
	 *               // augment the query via JPA Criteria API
	 *               query.where(builder.like(book.get(Book_.title), titlePattern)),
	 *                           builder.greaterThan(book.get(Book_.pages), minPages))
	 *                   .orderBy(builder.asc(book.get(Book_.isbn)))
	 *       .createQuery(session)
	 *       .getResultList();
	 * </pre>
	 * For complicated cases, a {@link org.hibernate.query.criteria.CriteriaDefinition}
	 * may be used within an augmentation to eliminate repetitive explicit references to
	 * the {@link CriteriaBuilder}.
	 * <pre>
	 * SelectionSpecification.create(Book.class)
	 *       .addAugmentation((builder, query, book) ->
	 *           // eliminate explicit references to 'builder'
	 *           new CriteriaDefinition<>(query) {{
	 *               where(like(entity.get(BasicEntity_.title), titlePattern),
	 *                     greaterThan(book.get(Book_.pages), minPages));
	 *               orderBy(asc(book.get(Book_.isbn)));
	 *           }}
	 *       )
	 *       .createQuery(session)
	 *       .getResultList();
	 * </pre>
	 *
	 * @param augmentation A function capable of modifying or augmenting a criteria query.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> addAugmentation(Augmentation<T> augmentation);

	/**
	 * Covariant override.
	 */
	@Override
	SelectionQuery<T> createQuery(SharedSessionContract session);

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} for the given entity type,
	 * allowing the addition of {@linkplain SelectionSpecification#addOrdering sorting}
	 * and {@linkplain SelectionSpecification#addRestriction restrictions}.
	 * This is effectively the same as calling {@linkplain #create(Class, String)}
	 * with {@code "from {rootEntityType}"} as the HQL.
	 *
	 * @param rootEntityType The entity type which is the root of the query.
	 *
	 * @param <T> The entity type which is the root of the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 */
	static <T> SelectionSpecification<T> create(Class<T> rootEntityType) {
		return new SelectionSpecificationImpl<>( rootEntityType );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} based on a base HQL statement,
	 * allowing the addition of {@linkplain SelectionSpecification#addOrdering sorting}
	 * and {@linkplain SelectionSpecification#addRestriction restrictions}.
	 *
	 * @param hql The base HQL query.
	 * @param resultType The result type which will ultimately be returned from the {@linkplain SelectionQuery}
	 *
	 * @param <T> The root entity type for the query.
	 * {@code resultType} and {@code <T>} are both expected to refer to a singular query root.
	 *
	 * @throws IllegalSelectQueryException The given HQL is expected to be a {@code select} query.  This method will
	 * throw an exception if not.
	 */
	static <T> SelectionSpecification<T> create(Class<T> resultType, String hql) {
		return new SelectionSpecificationImpl<>( hql, resultType );
	}

	/**
	 * Returns a specification reference which can be used to programmatically,
	 * iteratively build a {@linkplain SelectionQuery} for the given criteria query,
	 * allowing the addition of {@linkplain SelectionSpecification#addOrdering sorting}
	 * and {@linkplain SelectionSpecification#addRestriction restrictions}.
	 *
	 * @param criteria The criteria query
	 *
	 * @param <T> The entity type which is the root of the query.
	 */
	static <T> SelectionSpecification<T> create(CriteriaQuery<T> criteria) {
		return new SelectionSpecificationImpl<>( criteria );
	}
}
