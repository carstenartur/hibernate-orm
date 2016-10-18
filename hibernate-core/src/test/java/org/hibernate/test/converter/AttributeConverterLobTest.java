/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import static org.junit.Assert.assertEquals;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.persistence.AttributeConverter;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * HHH-11098 Test to check the number of attributeconverter calls on a simple save and list
 *
 * @author Carsten Hammer
 */
public class AttributeConverterLobTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ EntityImpl.class };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

/**
 * 
 */
//	@Test
//	public void testsave_withoutImmutable() {
//		ConverterImpl.fromdatabasecounter = 0;
//		ConverterImpl.todatabasecounter = 0;
//		Session session = openSession();
//		session.beginTransaction();
//		EntityImpl object = new EntityImpl();
//		object.status = new HashMap<>();
//		object.status.put( "asdf", Integer.valueOf( 6 ) );
//		object.status.put( "key", "table" );
//		object.id = UUID.randomUUID();
//		session.save( object );
//		session.getTransaction().commit();
//		session.close();
//
//		/**
//		 * Ok, that is because of the missing Immutable annotation using HashMap
//		 */
//		assertEquals( 1, ConverterImpl.fromdatabasecounter );
//		assertEquals( 2, ConverterImpl.todatabasecounter );
//	}
	
	@Test
	public void testsave() {
		ConverterImpl.fromdatabasecounter = 0;
		ConverterImpl.todatabasecounter = 0;
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status = new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id = UUID.randomUUID();
		session.save( object );
		session.getTransaction().commit();
		session.close();

		/**
		 * fine!
		 */
		assertEquals( 0, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );
	}

	/**
	 * first save 
	 * then get 
	 * 
	 * Problem here is that the attributeconverter is processing a fromdatabaseconversion while it
	 * should just use the cached value
	 */
	@Test
	public void testsaveandget() {
		ConverterImpl.fromdatabasecounter = 0;
		ConverterImpl.todatabasecounter = 0;
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status = new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id = UUID.randomUUID();
		session.save( object );
		session.getTransaction().commit();
		session.close();

		/**
		 * fine!
		 */
		assertEquals( 0, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );

		session = openSession();
		session.beginTransaction();
		EntityImpl entityImpl = session.get( EntityImpl.class, object.id );
		session.getTransaction().commit();
		session.close();

		assertEquals( 1, ConverterImpl.todatabasecounter );
		/**
		 * why 1 and not 0? I pull this from the second level cache, right? Why does it need to be converted then? I do
		 * not want to cache the database representation but the java representation..
		 */
		assertEquals( 1, ConverterImpl.fromdatabasecounter );
		assertEquals( "table", entityImpl.status.get( "key" ) );
	}

	/**
	 * first save 
	 * then saveorupdate 
	 * 
	 * Problem here is that the attributeconverter is processing a fromdatabaseconversion while it
	 * should just use the cached value
	 */
	@Test
	public void testsaveandsaveorupdate() {
		ConverterImpl.fromdatabasecounter = 0;
		ConverterImpl.todatabasecounter = 0;
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status = new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id = UUID.randomUUID();
		session.save( object );
		session.getTransaction().commit();
		session.close();
		/**
		 * fine!
		 */
		assertEquals( 0, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );
		
		session = openSession();
		session.beginTransaction();
		/**
		 * Either save(Object) or update(Object) the given instance, depending upon resolution of the unsaved-value
		 * checks (see the manual for discussion of unsaved-value checking). This operation cascades to associated
		 * instances if the association is mapped with cascade="save-update"
		 */
		session.saveOrUpdate( object );
		session.getTransaction().commit();
		session.close();

		/**
		 * fine!
		 */
		assertEquals( 1, ConverterImpl.todatabasecounter );
		/**
		 * why 1 and not 0?
		 */
		assertEquals( 1, ConverterImpl.fromdatabasecounter );
	}

	@Test
	public void testsaveandmerge() {
		ConverterImpl.fromdatabasecounter = 0;
		ConverterImpl.todatabasecounter = 0;
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status = new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id = UUID.randomUUID();
		session.save( object );
		session.getTransaction().commit();
		session.close();
		/**
		 * fine!
		 */
		assertEquals( 0, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );

		session = openSession();
		session.beginTransaction();
		/**
		 * Copy the state of the given object onto the persistent object with the same identifier. If there is no persistent instance currently 
		 * associated with the session, it will be loaded. Return the persistent instance. If the given instance is unsaved, save a copy of 
		 * and return it as a newly persistent instance. The given instance does not become associated with the session. This operation 
		 * cascades to associated instances if the association is mapped with cascade="merge" 
		 */
		session.merge( object );
		session.getTransaction().commit();
		session.close();

		/**
		 * why 1 and not 0? Is it that as written in the merge javadoc merge cannot use a persistent instance from the second level cache - only 
		 * from the session? Does it make sense to ignore the second level cache in this case?
		 */
		assertEquals( 1, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );
	}
	
	@Test
	public void testsaveandquery() {
		ConverterImpl.fromdatabasecounter = 0;
		ConverterImpl.todatabasecounter = 0;
		Session session = openSession();
		session.beginTransaction();
		EntityImpl object = new EntityImpl();
		object.status = new MyMap<>();
		object.status.put( "asdf", Integer.valueOf( 6 ) );
		object.status.put( "key", "table" );
		object.id = UUID.randomUUID();
		session.save( object );
		session.getTransaction().commit();
		session.close();
		/**
		 * fine!
		 */
		assertEquals( 0, ConverterImpl.fromdatabasecounter );
		assertEquals( 1, ConverterImpl.todatabasecounter );
		
		session = openSession();
		session.beginTransaction();
		Query<EntityImpl> createQuery = session.createQuery( "select e from EntityImpl e where id=:id", EntityImpl.class );
		createQuery.setParameter( "id", object.id );
		List<EntityImpl> resultList = createQuery.getResultList();
		assertEquals( 1, resultList.size() );
		session.getTransaction().commit();
		session.close();

		/**
		 * Same question as before, why do I need a todatabase conversion here?
		 */
		assertEquals( 1, ConverterImpl.todatabasecounter );
		assertEquals( "table", resultList.get( 0 ).status.get( "key" ) );
		assertEquals( 1, ConverterImpl.fromdatabasecounter );
	}

	@Converter
	public static class ConverterImpl implements AttributeConverter<MyMap<String, Object>, byte[]> {

		public static int todatabasecounter = 0;
		public static int fromdatabasecounter = 0;

		@Override
		public byte[] convertToDatabaseColumn(MyMap<String, Object> map) {
			todatabasecounter++;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try ( XMLEncoder encoder = new XMLEncoder( out ) ) {
				encoder.writeObject( map );
				encoder.flush();
			}
			return out.toByteArray();
		}

		@Override
		public MyMap<String, Object> convertToEntityAttribute(byte[] dbData) {
			fromdatabasecounter++;
			try ( ByteArrayInputStream in = new ByteArrayInputStream( dbData );
					XMLDecoder decoder = new XMLDecoder( in ) ) {
				return (MyMap<String, Object>) decoder.readObject();
			}
			catch (IOException e) {
				return null;
			}
		}
	}

	@Entity(name = "EntityImpl")
	@Table(name = "EntityImpl")
	@Cacheable
	public static class EntityImpl implements Serializable {

		@Id
		private UUID id;

		@Column
		@Lob
		@Convert(converter = ConverterImpl.class)
		private MyMap<String, Object> status;	
	}

	@Immutable
	public static class MyMap<String, Object> extends HashMap<String, Object> {
	}
}
