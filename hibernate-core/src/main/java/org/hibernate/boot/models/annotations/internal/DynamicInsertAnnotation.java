/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class DynamicInsertAnnotation implements DynamicInsert {

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public DynamicInsertAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public DynamicInsertAnnotation(DynamicInsert annotation, SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public DynamicInsertAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return DynamicInsert.class;
	}
}
