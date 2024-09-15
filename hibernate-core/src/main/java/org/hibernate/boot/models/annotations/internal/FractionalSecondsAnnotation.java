/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.models.spi.SourceModelBuildingContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FractionalSecondsAnnotation implements FractionalSeconds {
	private int value;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FractionalSecondsAnnotation(SourceModelBuildingContext modelContext) {
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FractionalSecondsAnnotation(FractionalSeconds annotation, SourceModelBuildingContext modelContext) {
		this.value = annotation.value();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FractionalSecondsAnnotation(Map<String, Object> attributeValues, SourceModelBuildingContext modelContext) {
		this.value = (int) attributeValues.get( "value" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return FractionalSeconds.class;
	}

	@Override
	public int value() {
		return value;
	}

	public void value(int value) {
		this.value = value;
	}


}
