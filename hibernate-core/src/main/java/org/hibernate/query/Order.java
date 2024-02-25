/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;

import java.util.Objects;

/**
 * A rule for sorting a query result set.
 * <p>
 * This is a convenience class which allows query result ordering
 * rules to be passed around the system before being applied to
 * a {@link Query} by calling {@link SelectionQuery#setOrder}.
 * <p>
 * A parameter of a {@linkplain org.hibernate.annotations.processing.HQL
 * HQL query method} may be declared with type {@code Order<? super E>},
 * {@code List<Order<? super E>>}, or {@code Order<? super E>...} (varargs)
 * where {@code E} is the entity type returned by the query.
 *
 * @param <X> The result type of the query to be sorted
 *
 * @see SelectionQuery#setOrder(Order)
 * @see SelectionQuery#setOrder(java.util.List)
 *
 * @author Gavin King
 *
 * @since 6.3
 */
@Incubating
public class Order<X> {
	private final SortDirection order;
	private final SingularAttribute<X,?> attribute;
	private final Class<X> entityClass;
	private final String attributeName;
	private final NullPrecedence nullPrecedence;
	private final int element;
	private final boolean ignoreCase;

	private Order(SortDirection order, NullPrecedence nullPrecedence, SingularAttribute<X, ?> attribute) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, SingularAttribute<X, ?> attribute, boolean ignoreCase) {
		this.order = order;
		this.attribute = attribute;
		this.attributeName = attribute.getName();
		this.entityClass = attribute.getDeclaringType().getJavaType();
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, Class<X> entityClass, String attributeName) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, int element) {
		this.order = order;
		this.entityClass = null;
		this.attributeName = null;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = element;
		this.ignoreCase = false;
	}

	private Order(SortDirection order, NullPrecedence nullPrecedence, Class<X> entityClass, String attributeName, boolean ignoreCase) {
		this.order = order;
		this.entityClass = entityClass;
		this.attributeName = attributeName;
		this.attribute = null;
		this.nullPrecedence = nullPrecedence;
		this.element = 1;
		this.ignoreCase = ignoreCase;
	}

	public static <T> Order<T> asc(SingularAttribute<T,?> attribute) {
		return new Order<>(SortDirection.ASCENDING, NullPrecedence.NONE, attribute);
	}

	public static <T> Order<T> desc(SingularAttribute<T,?> attribute) {
		return new Order<>(SortDirection.DESCENDING, NullPrecedence.NONE, attribute);
	}

	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction) {
		return new Order<>(direction, NullPrecedence.NONE, attribute);
	}

	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, boolean ignoreCase) {
		return new Order<>(direction, NullPrecedence.NONE, attribute, ignoreCase);
	}

	public static <T> Order<T> by(SingularAttribute<T,?> attribute, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>(direction, nullPrecedence, attribute);
	}

	public static <T> Order<T> asc(Class<T> entityClass, String attributeName) {
		return new Order<>( SortDirection.ASCENDING, NullPrecedence.NONE, entityClass, attributeName );
	}

	public static <T> Order<T> desc(Class<T> entityClass, String attributeName) {
		return new Order<>( SortDirection.DESCENDING, NullPrecedence.NONE, entityClass, attributeName );
	}

	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction) {
		return new Order<>( direction, NullPrecedence.NONE, entityClass, attributeName );
	}

	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, boolean ignoreCase) {
		return new Order<>( direction, NullPrecedence.NONE, entityClass, attributeName, ignoreCase );
	}

	public static <T> Order<T> by(Class<T> entityClass, String attributeName, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>( direction, nullPrecedence, entityClass, attributeName );
	}

	public static Order<Object[]> asc(int element) {
		return new Order<>( SortDirection.ASCENDING, NullPrecedence.NONE, element );
	}

	public static Order<Object[]> desc(int element) {
		return new Order<>( SortDirection.DESCENDING, NullPrecedence.NONE, element );
	}

	public static Order<Object[]> by(int element, SortDirection direction) {
		return new Order<>( direction, NullPrecedence.NONE, element );
	}

	public static Order<Object[]> by(int element, SortDirection direction, NullPrecedence nullPrecedence) {
		return new Order<>( direction, nullPrecedence, element );
	}

	public SortDirection getDirection() {
		return order;
	}

	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}

	public boolean isCaseInsensitive() {
		return ignoreCase;
	}

	public SingularAttribute<X, ?> getAttribute() {
		return attribute;
	}

	public Class<X> getEntityClass() {
		return entityClass;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public int getElement() {
		return element;
	}

	@Override
	public String toString() {
		return attributeName + " " + order;
	}

	@Override
	public boolean equals(Object o) {
		if ( o instanceof Order) {
			Order<?> that = (Order<?>) o;
			return that.order == this.order
				&& that.nullPrecedence == this.nullPrecedence
				&& that.element == this.element
				&& Objects.equals( that.attributeName, this.attributeName )
				&& Objects.equals( that.entityClass, this.entityClass );
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash( order, element, attributeName, entityClass );
	}
}
