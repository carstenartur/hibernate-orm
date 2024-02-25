package org.hibernate.jpamodelgen.test.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.SelectionQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface Dao {

    EntityManager getEntityManager();

    @Find
    Book getBook(String isbn);

    @Find(enabledFetchProfiles="Goodbye")
    Book getBookFetching(String isbn);

    @Find
    Book getBook(String title, String author);

    @Find(enabledFetchProfiles="Hello")
    Book getBookFetching(String title, String author);

    @Find
    Book getBook(String title, String isbn, String author);

    @Find
    List<Book> getBooks(String title);

    @Find(enabledFetchProfiles="Hello")
    List<Book> getBooksFetching(String title);

    @Find
    List<Book> getBooks(String title, Page page, Order<Book> order);

    @Find
    SelectionQuery<Book> createBooksSelectionQuery(String title);

    @HQL("where title like ?1")
    List<Book> findBooksByTitle(String title);

    @HQL("from Book where title like ?1")
    TypedQuery<Book> findByTitle(String title);

    @HQL("from Book where title like ?1")
    SelectionQuery<Book> findByTitleSelectionQuery(String title);

    @HQL("from Book where title like ?1 order by title fetch first ?2 rows only")
    List<Book> findFirstNByTitle(String title, int N);
//
//    @HQL("from Book where title like :title")
//    List<Book> findByTitleWithPagination(String title, Order<? super Book> order, Page page);
//
//    @HQL("from Book where title like :title")
//    SelectionQuery<Book> findByTitleWithOrdering(String title, List<Order<? super Book>> order);
//
//    @HQL("from Book where title like :title")
//    SelectionQuery<Book> findByTitleWithOrderingByVarargs(String title, Order<? super Book>... order);

    @HQL("select count(*) from Book")
    long countBooks();

    @HQL("select count(*)>1 from Book")
    boolean booksExist();

    @HQL("delete from Book")
    int deleteBooks();

    @HQL("delete from Book book where book.isbn=:isbn")
    boolean deleteBook(String isbn);

    @HQL("select count(*), count(*)>1 from Book")
    Object[] funnyQueryReturningArray();

    class Record {
        Record(Long count, Boolean exists) {}
    }
    @HQL("select count(*), count(*)>1 from Book")
    Record funnyQueryReturningRecord();

    @HQL("from Book where isbn = :isbn")
    Book findByIsbn(String isbn);

    @SQL("select * from Book where isbn = :isbn")
    Book findByIsbnNative(String isbn);

    @Find
    Bean beanByIdProperty(Long key);

    @Find
    List<Bean> beansForText(String text);

    @HQL("where isbn = ?1")
    List<Book> sortedBooksForIsbn(String isbn, Order<? super Book>... order);

    @Find
    List<Book> sortedBooks(String isbn, Order<? super Book>... order);

    @HQL("select local date")
    LocalDate localDate();

    @HQL("select local datetime")
    LocalDateTime localDatetime();

    @HQL("select avg(pages) from Book")
    double averagePageCount();

    @HQL("select b\nfrom Book b\nwhere b.isbn = :isbn")
    Book findByIsbnMultiline(String isbn);
}
