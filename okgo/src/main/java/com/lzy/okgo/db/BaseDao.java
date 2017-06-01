/*
 * Copyright 2016 jeasonlzy(廖子尧)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzy.okgo.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lzy.okgo.utils.OkLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ================================================
 * 作    者：jeasonlzy（廖子尧）Github地址：https://github.com/jeasonlzy
 * 版    本：1.0
 * 创建日期：16/9/11
 * 描    述：
 * 修订历史：
 * ================================================
 */
public abstract class BaseDao<T> {

    private ReentrantLock lock;
    private SQLiteOpenHelper helper;
    private SQLiteDatabase database;

    public BaseDao(SQLiteOpenHelper helper) {
        this.helper = helper;
        lock = new ReentrantLock();
        database = openWriter();
    }

    /** 获取对应的表名 */
    protected abstract String getTableName();

    /** 将Cursor解析成对应的JavaBean */
    public abstract T parseCursorToBean(Cursor cursor);

    /** 需要替换的列 */
    public abstract ContentValues getContentValues(T t);

    protected final SQLiteDatabase openReader() {
        return helper.getReadableDatabase();
    }

    protected final SQLiteDatabase openWriter() {
        return helper.getWritableDatabase();
    }

    protected final void closeDatabase(SQLiteDatabase database, Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) cursor.close();
        if (database != null && database.isOpen()) database.close();
    }

    /** 需要数据库中有个 _id 的字段 */
    public int count() {
        return countColumn("_id");
    }

    /** 返回一列的总记录数量 */
    public int countColumn(String columnName) {
        lock.lock();
        String sql = "SELECT COUNT(?) FROM " + getTableName();
        Cursor cursor = null;
        try {
            database.beginTransaction();
            cursor = database.rawQuery(sql, new String[]{columnName});
            int count = 0;
            if (cursor.moveToNext()) {
                count = cursor.getInt(0);
            }
            database.setTransactionSuccessful();
            return count;
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, cursor);
            lock.unlock();
        }
        return 0;
    }

    /** 创建一条记录 */
    public long insert(T t) {
        lock.lock();
        try {
            database.beginTransaction();
            long id = database.insert(getTableName(), null, getContentValues(t));
            database.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, null);
            lock.unlock();
        }
        return 0;
    }

    /** 删除所有数据 */
    public int deleteAll() {
        return delete(null, null);
    }

    /** 根据条件删除数据库中的数据 */
    public int delete(String whereClause, String[] whereArgs) {
        lock.lock();
        try {
            database.beginTransaction();
            int result = database.delete(getTableName(), whereClause, whereArgs);
            database.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, null);
            lock.unlock();
        }
        return 0;
    }

    /** 更新一条记录 */
    public int update(T t, String whereClause, String[] whereArgs) {
        lock.lock();
        try {
            database.beginTransaction();
            int count = database.update(getTableName(), getContentValues(t), whereClause, whereArgs);
            database.setTransactionSuccessful();
            return count;
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, null);
            lock.unlock();
        }
        return 0;
    }

    /**
     * replace 语句有如下行为特点
     * 1. replace语句会删除原有的一条记录， 并且插入一条新的记录来替换原记录。
     * 2. 一般用replace语句替换一条记录的所有列， 如果在replace语句中没有指定某列， 在replace之后这列的值被置空 。
     * 3. replace语句根据主键的值确定被替换的是哪一条记录
     * 4. 如果执行replace语句时， 不存在要替换的记录， 那么就会插入一条新的记录。
     * 5. replace语句不能根据where子句来定位要被替换的记录
     * 6. 如果新插入的或替换的记录中， 有字段和表中的其他记录冲突， 那么会删除那条其他记录。
     */
    public long replace(T t) {
        lock.lock();
        try {
            database.beginTransaction();
            long id = database.replace(getTableName(), null, getContentValues(t));
            database.setTransactionSuccessful();
            return id;
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, null);
            lock.unlock();
        }
        return 0;
    }

    /** 查询并返回所有对象的集合 */
    public List<T> queryAll() {
        return query(null, null);
    }

    /** 按条件查询对象并返回集合 */
    public List<T> query(String selection, String[] selectionArgs) {
        return query(null, selection, selectionArgs, null, null, null, null);
    }

    /** 按条件查询对象并返回集合 */
    public List<T> query(String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
        lock.lock();
        List<T> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            database.beginTransaction();
            cursor = database.query(getTableName(), columns, selection, selectionArgs, groupBy, having, orderBy, limit);
            while (!cursor.isClosed() && cursor.moveToNext()) {
                list.add(parseCursorToBean(cursor));
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            OkLogger.printStackTrace(e);
        } finally {
            database.endTransaction();
            closeDatabase(null, cursor);
            lock.unlock();
        }
        return list;
    }
}