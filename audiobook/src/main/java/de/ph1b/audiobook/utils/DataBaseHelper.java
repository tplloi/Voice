package de.ph1b.audiobook.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import de.ph1b.audiobook.BuildConfig;

public class DataBaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "audioBookDB";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_MEDIA = "TABLE_MEDIA";
    private static final String TABLE_BOOKS = "TABLE_BOOKS";

    private static final String KEY_MEDIA_ID = "KEY_MEDIA_ID";
    private static final String KEY_MEDIA_PATH = "KEY_MEDIA_PATH";
    private static final String KEY_MEDIA_NAME = "KEY_MEDIA_NAME";
    private static final String KEY_MEDIA_DURATION = "KEY_MEDIA_DURATION";
    private static final String KEY_MEDIA_BOOK_ID = "KEY_MEDIA_BOOK_ID";

    private String CREATE_MEDIA_TABLE = "CREATE TABLE " + TABLE_MEDIA + " ( " +
            KEY_MEDIA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            KEY_MEDIA_PATH + " TEXT, " +
            KEY_MEDIA_NAME + " TEXT, " +
            KEY_MEDIA_DURATION + " INTEGER, " +
            KEY_MEDIA_BOOK_ID + " INTEGER)";

    private static final String KEY_BOOK_ID = "KEY_BOOK_ID";
    private static final String KEY_BOOK_NAME = "KEY_BOOK_NAME";
    private static final String KEY_BOOK_COVER = "KEY_BOOK_COVER";
    private static final String KEY_BOOK_CURRENT_MEDIA_ID = "KEY_BOOK_CURRENT_MEDIA_ID";
    private static final String KEY_BOOK_CURRENT_MEDIA_POSITION = "KEY_BOOK_CURRENT_MEDIA_POSITION";
    private static final String KEY_BOOK_THUMB = "KEY_BOOK_THUMB";
    private static final String KEY_BOOK_SORT_ID = "KEY_BOOK_SORT_ID";

    private String CREATE_BOOK_TABLE = "CREATE TABLE " + TABLE_BOOKS + " ( " +
            KEY_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            KEY_BOOK_NAME + " TEXT, " +
            KEY_BOOK_COVER + " TEXT, " +
            KEY_BOOK_CURRENT_MEDIA_ID + " INTEGER, " +
            KEY_BOOK_CURRENT_MEDIA_POSITION + " INTEGER, " +
            KEY_BOOK_THUMB + " TEXT, " +
            KEY_BOOK_SORT_ID + " INTEGER)";

    private static DataBaseHelper instance;

    public static synchronized DataBaseHelper getInstance(Context context) {
        if (instance == null)
            instance = new DataBaseHelper(context);
        return instance;
    }

    private DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(CREATE_MEDIA_TABLE);
            db.execSQL(CREATE_BOOK_TABLE);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void upgradeOne(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            String CREATE_MEDIA_TABLE = "CREATE TABLE " + "TABLE_MEDIA" + " ( " +
                    "KEY_MEDIA_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_MEDIA_PATH" + " TEXT, " +
                    "KEY_MEDIA_NAME" + " TEXT, " +
                    "KEY_MEDIA_POSITION" + " INTEGER, " +
                    "KEY_MEDIA_DURATION" + " INTEGER, " +
                    "KEY_MEDIA_BOOK_ID" + " INTEGER" +
                    ")";

            String CREATE_BOOK_TABLE = "CREATE TABLE " + "TABLE_BOOKS" + " ( " +
                    "KEY_BOOK_ID" + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "KEY_BOOK_NAME" + " TEXT, " +
                    "KEY_BOOK_COVER" + " TEXT, " +
                    "KEY_BOOK_THUMB" + " TEXT, " +
                    "KEY_BOOK_POSITION" + " INTEGER, " +
                    "KEY_BOOK_SORT_ID" + " INTEGER"
                    + ")";


            //first rename old tables
            db.execSQL("ALTER TABLE mediaTable RENAME TO tempMediaTable");
            db.execSQL("ALTER TABLE bookTable RENAME TO tempBookTable");

            //now create new tables
            db.execSQL(CREATE_MEDIA_TABLE);
            db.execSQL(CREATE_BOOK_TABLE);

            //now getting book table
            Cursor bookTableCursor = db.query("tempBookTable",
                    new String[]{"bookName", "bookCover", "bookMediaContaining", "bookPosition", "bookThumb"},
                    null, null, null, null, null);
            while (bookTableCursor.moveToNext()) {
                String bookName = bookTableCursor.getString(0);
                String bookCover = bookTableCursor.getString(1);
                String bookMediaContaining = bookTableCursor.getString(2);
                int bookPosition = bookTableCursor.getInt(3);
                String bookThumb = bookTableCursor.getString(4);

                //adding book in new table
                ContentValues bookValues = new ContentValues();
                bookValues.put("KEY_BOOK_NAME", bookName);
                bookValues.put("KEY_BOOK_COVER", bookCover);
                bookValues.put("KEY_BOOK_THUMB", bookThumb);
                bookValues.put("KEY_BOOK_POSITION", bookPosition);
                long newBookId = db.insert("TABLE_BOOKS", null, bookValues);
                bookValues.put("KEY_BOOK_SORT_ID", newBookId);
                db.update("TABLE_BOOKS", bookValues, "KEY_BOOK_ID" + " = " + newBookId, null);

                //generate int array from string
                String[] mediaIDsAsSplittedString = bookMediaContaining.split(",");
                int[] mediaIDsAsSplittedInt = new int[mediaIDsAsSplittedString.length];
                for (int i = 0; i < mediaIDsAsSplittedInt.length; i++) {
                    mediaIDsAsSplittedInt[i] = Integer.parseInt(mediaIDsAsSplittedString[i]);
                }

                for (int i : mediaIDsAsSplittedInt) {
                    Cursor mediaTableCursor = db.query("tempMediaTable",
                            new String[]{"mediaPath", "mediaName", "mediaPosition", "mediaDuration"},
                            "mediaID = " + i, null, null, null, null);
                    if (mediaTableCursor.moveToFirst()) {
                        String mediaPath = mediaTableCursor.getString(0);
                        String mediaName = mediaTableCursor.getString(1);
                        int mediaPosition = mediaTableCursor.getInt(2);
                        int mediaDuration = mediaTableCursor.getInt(3);

                        //adding these values in new media table
                        ContentValues mediaValues = new ContentValues();
                        mediaValues.put("KEY_MEDIA_PATH", mediaPath);
                        mediaValues.put("KEY_MEDIA_NAME", mediaName);
                        mediaValues.put("KEY_MEDIA_POSITION", mediaPosition);
                        mediaValues.put("KEY_MEDIA_DURATION", mediaDuration);
                        mediaValues.put("KEY_MEDIA_BOOK_ID", newBookId);
                        db.insert("TABLE_MEDIA", null, mediaValues);
                    }
                }

            }
            //dropping temporary tables
            db.execSQL("DROP TABLE tempMediaTable");
            db.execSQL("DROP TABLE tempBookTable");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void upgradeTwo(SQLiteDatabase db) {
        // first rename old tables
        db.execSQL("ALTER TABLE TABLE_MEDIA RENAME TO TEMP_TABLE_MEDIA");
        db.execSQL("ALTER TABLE TABLE_BOOKS RENAME TO TEMP_TABLE_BOOKS");

        // new create new tables
        db.execSQL(CREATE_MEDIA_TABLE);
        db.execSQL(CREATE_BOOK_TABLE);

        // getting data from old table
        Cursor bookTableCursor = db.query("TEMP_TABLE_BOOKS",
                new String[]{"KEY_BOOK_ID", "KEY_BOOK_NAME", "KEY_BOOK_COVER", "KEY_BOOK_POSITION", "KEY_BOOK_THUMB", "KEY_BOOK_SORT_ID"},
                null, null, null, null, null);

        //going through all books and updating them with current values
        while (bookTableCursor.moveToNext()) {
            int oldBookId = bookTableCursor.getInt(0);
            String bookName = bookTableCursor.getString(1);
            String bookCover = bookTableCursor.getString(2);
            int bookPosition = bookTableCursor.getInt(3);
            String bookThumb = bookTableCursor.getString(4);
            int bookSortId = bookTableCursor.getInt(5);

            Cursor mediaPositionCursor = db.query("TEMP_TABLE_MEDIA",
                    new String[]{"KEY_MEDIA_POSITION"},
                    "KEY_MEDIA_BOOK_ID = " + oldBookId,
                    null, null, null, null, null);
            int bookMediaPosition = 0;
            if (mediaPositionCursor.moveToFirst())
                bookMediaPosition = mediaPositionCursor.getInt(0);
            ContentValues bookValues = new ContentValues();
            bookValues.put("KEY_BOOK_NAME", bookName);
            bookValues.put("KEY_BOOK_COVER", bookCover);
            bookValues.put("KEY_BOOK_CURRENT_MEDIA_ID", bookPosition);
            bookValues.put("KEY_BOOK_CURRENT_MEDIA_POSITION", bookMediaPosition);
            bookValues.put("KEY_BOOK_THUMB", bookThumb);
            bookValues.put("KEY_BOOK_SORT_ID", bookSortId);

            int newBookId = (int) db.insert("TABLE_BOOKS", null, bookValues);

            Cursor mediaTableCursor = db.query("TEMP_TABLE_MEDIA",
                    new String[]{"KEY_MEDIA_PATH", "KEY_MEDIA_NAME", "KEY_MEDIA_DURATION"},
                    "KEY_MEDIA_BOOK_ID = " + oldBookId,
                    null, null, null, null);
            while (mediaTableCursor.moveToNext()) {
                String mediaPath = mediaTableCursor.getString(0);
                String mediaName = mediaTableCursor.getString(1);
                int mediaDuration = mediaTableCursor.getInt(2);
                ContentValues mediaValues = new ContentValues();
                mediaValues.put("KEY_MEDIA_PATH", mediaPath);
                mediaValues.put("KEY_MEDIA_NAME", mediaName);
                mediaValues.put("KEY_MEDIA_DURATION", mediaDuration);
                mediaValues.put("KEY_MEDIA_BOOK_ID", newBookId);
                db.insert("TABLE_MEDIA", null, mediaValues);
            }
        }

        // dropping old table
        db.execSQL("DROP TABLE TEMP_TABLE_MEDIA");
        db.execSQL("DROP TABLE TEMP_TABLE_BOOKS");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (BuildConfig.DEBUG)
            Log.d("dbh", "onUpgrade called with " + String.valueOf(oldVersion) + "/" + String.valueOf(newVersion));

        while (oldVersion++ < newVersion) {
            switch (oldVersion) {
                case 1:
                    upgradeOne(db);
                    break;
                case 2:
                    upgradeTwo(db);
                    break;
            }
        }
    }


    public BookDetail getBook(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_BOOKS,
                null,
                KEY_BOOK_ID + " = " + id,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            BookDetail book = new BookDetail();
            book.setId(Integer.parseInt(cursor.getString(0)));
            book.setName(cursor.getString(1));
            book.setCover(cursor.getString(2));
            book.setCurrentMediaId(cursor.getInt(3));
            book.setCurrentMediaPosition(cursor.getInt(4));
            book.setThumb(cursor.getString(5));
            book.setSortId(cursor.getInt(6));

            cursor.close();
            return book;
        }
        return null;
    }

    /*
    returns the progress of a book in °/°°
     */
    public int getGlobalProgress(BookDetail book) {
        long duration = 0;
        long progress = 0;
        boolean progressFound = false;

        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor mediaCursor = db.query(TABLE_MEDIA,
                    new String[]{KEY_MEDIA_ID, KEY_MEDIA_DURATION},
                    KEY_MEDIA_BOOK_ID + " = " + book.getId(),
                    null, null, null, KEY_MEDIA_ID);
            if (mediaCursor != null) {
                // adds the sum of length and the sum of played time
                while (mediaCursor.moveToNext()) {
                    duration += mediaCursor.getInt(1);
                    if (!progressFound) {
                        if (mediaCursor.getInt(0) == book.getCurrentMediaId()) {
                            progress += book.getCurrentMediaPosition();
                            progressFound = true;
                        } else
                            progress += mediaCursor.getInt(1);
                    }
                }
                if (!progressFound)
                    progress = 0;
                mediaCursor.close();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (duration == 0 || progress == 0)
            return 0;
        else {
            return (int) (progress * 1000 / duration);
        }
    }

    @SuppressWarnings("unchecked")
    public void updateBooksAsync(final ArrayList<BookDetail> books) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = DataBaseHelper.this.getWritableDatabase();
                db.beginTransaction();
                try {
                    for (BookDetail b : books) {
                        ContentValues values = new ContentValues();
                        values.put(KEY_BOOK_NAME, b.getName());
                        values.put(KEY_BOOK_COVER, b.getCover());
                        values.put(KEY_BOOK_CURRENT_MEDIA_ID, b.getCurrentMediaId());
                        values.put(KEY_BOOK_CURRENT_MEDIA_POSITION, b.getCurrentMediaPosition());
                        values.put(KEY_BOOK_THUMB, b.getThumb());
                        values.put(KEY_BOOK_SORT_ID, b.getSortId());
                        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + b.getId(), null);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return null;
            }
        }.execute();
    }


    public void addMedia(ArrayList<MediaDetail> media) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (MediaDetail m : media) {
                ContentValues values = new ContentValues();
                values.put(KEY_MEDIA_PATH, m.getPath()); // get title
                values.put(KEY_MEDIA_NAME, m.getName());
                values.put(KEY_MEDIA_DURATION, m.getDuration());
                values.put(KEY_MEDIA_BOOK_ID, m.getBookId());
                db.insert(TABLE_MEDIA, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }


    public int addBook(BookDetail book) {
        ContentValues values = new ContentValues();
        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_THUMB, book.getThumb());
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        int bookId = 0;
        try {
            bookId = (int) db.insert(TABLE_BOOKS, null, values);
            values.put(KEY_BOOK_SORT_ID, bookId);
            db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + bookId, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return bookId;
    }


    public ArrayList<MediaDetail> getMediaFromBook(int bookId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_MEDIA,
                null,
                KEY_MEDIA_BOOK_ID + " = " + bookId,
                null, null, null,
                KEY_MEDIA_ID);

        if (cursor != null) {
            ArrayList<MediaDetail> containingMedia = new ArrayList<MediaDetail>();
            while (cursor.moveToNext()) {
                MediaDetail media = new MediaDetail();
                media.setId(Integer.parseInt(cursor.getString(0)));
                media.setPath(cursor.getString(1));
                media.setName(cursor.getString(2));
                media.setDuration(Integer.parseInt(cursor.getString(3)));
                media.setBookId(Integer.parseInt(cursor.getString(4)));
                containingMedia.add(media);
            }
            cursor.close();
            return containingMedia;
        }
        return null;
    }


    public ArrayList<BookDetail> getAllBooks() {
        ArrayList<BookDetail> allBooks = new ArrayList<BookDetail>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BOOKS,
                null, null, null, null, null,
                KEY_BOOK_SORT_ID
        );

        BookDetail book;
        if (cursor.moveToFirst()) {
            do {
                book = new BookDetail();
                book.setId(Integer.parseInt(cursor.getString(0)));
                book.setName(cursor.getString(1));
                book.setCover(cursor.getString(2));
                book.setCurrentMediaId(cursor.getInt(3));
                book.setCurrentMediaPosition(cursor.getInt(4));
                book.setThumb(cursor.getString(5));
                book.setSortId(cursor.getInt(6));
                allBooks.add(book);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return allBooks;
    }


    public void updateBook(BookDetail book) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_BOOK_NAME, book.getName());
        values.put(KEY_BOOK_COVER, book.getCover());
        values.put(KEY_BOOK_CURRENT_MEDIA_ID, book.getCurrentMediaId());
        values.put(KEY_BOOK_CURRENT_MEDIA_POSITION, book.getCurrentMediaPosition());
        values.put(KEY_BOOK_THUMB, book.getThumb());
        values.put(KEY_BOOK_SORT_ID, book.getSortId());

        db.update(TABLE_BOOKS, values, KEY_BOOK_ID + " = " + book.getId(), null);
    }


    public void updateBookAsync(BookDetail book) {
        new AsyncTask<BookDetail, Void, Void>() {
            @Override
            protected Void doInBackground(BookDetail... params) {
                updateBook(params[0]);
                return null;
            }
        }.execute(book);
    }


    public void deleteBooksAsync(ArrayList<BookDetail> books) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (BookDetail b : books) {
                int bookId = b.getId();
                db.delete(TABLE_MEDIA,
                        KEY_MEDIA_BOOK_ID + " = " + bookId,
                        null);
                db.delete(TABLE_BOOKS,
                        KEY_BOOK_ID + " = " + bookId,
                        null);

                if (b.getCover() != null) {
                    CommonTasks.deleteFile(new File(b.getCover()));
                }

                if (b.getThumb() != null) {
                    CommonTasks.deleteFile(new File(b.getThumb()));
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}

