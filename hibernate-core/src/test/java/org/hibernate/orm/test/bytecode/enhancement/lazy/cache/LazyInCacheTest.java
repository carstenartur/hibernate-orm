/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.usertype.UserTypeLegacyBridge;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class LazyInCacheTest extends BaseCoreFunctionalTestCase {

    private Long orderId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Order.class, Product.class, Tag.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true );
    }

    @Before
    public void prepare() {
        Order order = new Order();
        Product product = new Product();
        order.products.add( product );
        order.data = "some data".getBytes( Charset.defaultCharset() );

        doInJPA( this::sessionFactory, em -> {
            em.persist( product );
            em.persist( order );
        } );

        orderId = order.id;
    }

    @Test
    public void test() {
        doInJPA( this::sessionFactory, em -> {
            Order order = em.find( Order.class, orderId );
            Assert.assertEquals( 1, order.products.size() );
        } );
    }

    // --- //

    @Entity(name = "Order")
    @Table( name = "ORDER_TABLE" )
    @Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
    private static class Order {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @OneToMany
        List<Product> products = new ArrayList<>();

        @OneToMany
        List<Tag> tags = new ArrayList<>();

        @Basic( fetch = FetchType.LAZY )
        @Type( BinaryCustomType.class )
//        @JdbcTypeCode(Types.LONGVARBINARY)
        byte[] data;
    }

    @Entity(name = "Product")
    @Table( name = "PRODUCT" )
    private static class Product {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        String name;
    }

    @Entity(name = "Tag")
    @Table( name = "TAG" )
    private static class Tag {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        String name;
    }

    public static class BinaryCustomType extends UserTypeLegacyBridge {
        public BinaryCustomType() {
            super( "binary" );
        }
    }
}
