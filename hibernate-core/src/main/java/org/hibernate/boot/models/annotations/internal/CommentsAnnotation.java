/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Comments;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.spi.RepeatableContainer;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class CommentsAnnotation implements Comments, RepeatableContainer<org.hibernate.annotations.Comment> {

	private org.hibernate.annotations.Comment[] value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public CommentsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public CommentsAnnotation(Comments annotation, SourceModelBuildingContext modelContext) {
		this.value = extractJdkValue( annotation, HibernateAnnotations.COMMENTS, "value", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public CommentsAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (org.hibernate.annotations.Comment[]) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Comments.class;
	}

	@Override
	public org.hibernate.annotations.Comment[] value() {
		return value;
	}

	public void value(org.hibernate.annotations.Comment[] value) {
		this.value = value;
	}


}
