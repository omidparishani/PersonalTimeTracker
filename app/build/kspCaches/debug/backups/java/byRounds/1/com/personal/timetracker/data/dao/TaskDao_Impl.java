package com.personal.timetracker.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.personal.timetracker.data.entity.TaskEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TaskDao_Impl implements TaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TaskEntity> __insertionAdapterOfTaskEntity;

  private final EntityDeletionOrUpdateAdapter<TaskEntity> __deletionAdapterOfTaskEntity;

  private final EntityDeletionOrUpdateAdapter<TaskEntity> __updateAdapterOfTaskEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTaskEntity = new EntityInsertionAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `tasks` (`id`,`jiraNumber`,`projectName`,`taskTitle`,`description`,`requiredMinutes`,`remainingMinutes`,`status`,`isRunning`,`runStartedAt`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getJiraNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getJiraNumber());
        }
        statement.bindString(3, entity.getProjectName());
        statement.bindString(4, entity.getTaskTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        statement.bindLong(6, entity.getRequiredMinutes());
        statement.bindLong(7, entity.getRemainingMinutes());
        statement.bindString(8, entity.getStatus());
        final int _tmp = entity.isRunning() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getRunStartedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getRunStartedAt());
        }
        statement.bindString(11, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfTaskEntity = new EntityDeletionOrUpdateAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `tasks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfTaskEntity = new EntityDeletionOrUpdateAdapter<TaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `tasks` SET `id` = ?,`jiraNumber` = ?,`projectName` = ?,`taskTitle` = ?,`description` = ?,`requiredMinutes` = ?,`remainingMinutes` = ?,`status` = ?,`isRunning` = ?,`runStartedAt` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TaskEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getJiraNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getJiraNumber());
        }
        statement.bindString(3, entity.getProjectName());
        statement.bindString(4, entity.getTaskTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        statement.bindLong(6, entity.getRequiredMinutes());
        statement.bindLong(7, entity.getRemainingMinutes());
        statement.bindString(8, entity.getStatus());
        final int _tmp = entity.isRunning() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getRunStartedAt() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getRunStartedAt());
        }
        statement.bindString(11, entity.getCreatedAt());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tasks";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TaskEntity item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTaskEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final TaskEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTaskEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final TaskEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfTaskEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TaskEntity>> getAll() {
    final String _sql = "SELECT * FROM tasks ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<TaskEntity>> $completion) {
    final String _sql = "SELECT * FROM tasks ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TaskEntity>> getActive() {
    final String _sql = "SELECT * FROM tasks WHERE status != 'done' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TaskEntity>> getByStatus(final String status) {
    final String _sql = "SELECT * FROM tasks WHERE status = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final long id, final Continuation<? super TaskEntity> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRunning(final Continuation<? super TaskEntity> $completion) {
    final String _sql = "SELECT * FROM tasks WHERE isRunning = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<TaskEntity> observeRunning() {
    final String _sql = "SELECT * FROM tasks WHERE isRunning = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<TaskEntity>() {
      @Override
      @Nullable
      public TaskEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final TaskEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _result = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<TaskEntity>> search(final String q) {
    final String _sql = "SELECT * FROM tasks WHERE jiraNumber LIKE '%' || ? || '%' OR projectName LIKE '%' || ? || '%' OR taskTitle LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%' ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindString(_argIndex, q);
    _argIndex = 2;
    _statement.bindString(_argIndex, q);
    _argIndex = 3;
    _statement.bindString(_argIndex, q);
    _argIndex = 4;
    _statement.bindString(_argIndex, q);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tasks"}, new Callable<List<TaskEntity>>() {
      @Override
      @NonNull
      public List<TaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfJiraNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "jiraNumber");
          final int _cursorIndexOfProjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "projectName");
          final int _cursorIndexOfTaskTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "taskTitle");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRequiredMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "requiredMinutes");
          final int _cursorIndexOfRemainingMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "remainingMinutes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfIsRunning = CursorUtil.getColumnIndexOrThrow(_cursor, "isRunning");
          final int _cursorIndexOfRunStartedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "runStartedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<TaskEntity> _result = new ArrayList<TaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TaskEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpJiraNumber;
            if (_cursor.isNull(_cursorIndexOfJiraNumber)) {
              _tmpJiraNumber = null;
            } else {
              _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            }
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final String _tmpTaskTitle;
            _tmpTaskTitle = _cursor.getString(_cursorIndexOfTaskTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final int _tmpRequiredMinutes;
            _tmpRequiredMinutes = _cursor.getInt(_cursorIndexOfRequiredMinutes);
            final int _tmpRemainingMinutes;
            _tmpRemainingMinutes = _cursor.getInt(_cursorIndexOfRemainingMinutes);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final boolean _tmpIsRunning;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsRunning);
            _tmpIsRunning = _tmp != 0;
            final String _tmpRunStartedAt;
            if (_cursor.isNull(_cursorIndexOfRunStartedAt)) {
              _tmpRunStartedAt = null;
            } else {
              _tmpRunStartedAt = _cursor.getString(_cursorIndexOfRunStartedAt);
            }
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            _item = new TaskEntity(_tmpId,_tmpJiraNumber,_tmpProjectName,_tmpTaskTitle,_tmpDescription,_tmpRequiredMinutes,_tmpRemainingMinutes,_tmpStatus,_tmpIsRunning,_tmpRunStartedAt,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM tasks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object projectSummary(final Continuation<? super List<ProjectSum>> $completion) {
    final String _sql = "SELECT projectName, SUM(requiredMinutes - remainingMinutes) as total FROM tasks GROUP BY projectName ORDER BY total DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProjectSum>>() {
      @Override
      @NonNull
      public List<ProjectSum> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfProjectName = 0;
          final int _cursorIndexOfTotal = 1;
          final List<ProjectSum> _result = new ArrayList<ProjectSum>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProjectSum _item;
            final String _tmpProjectName;
            _tmpProjectName = _cursor.getString(_cursorIndexOfProjectName);
            final int _tmpTotal;
            _tmpTotal = _cursor.getInt(_cursorIndexOfTotal);
            _item = new ProjectSum(_tmpProjectName,_tmpTotal);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object jiraSummary(final Continuation<? super List<JiraSum>> $completion) {
    final String _sql = "SELECT jiraNumber, SUM(requiredMinutes - remainingMinutes) as total FROM tasks WHERE jiraNumber IS NOT NULL AND jiraNumber != '' GROUP BY jiraNumber ORDER BY total DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<JiraSum>>() {
      @Override
      @NonNull
      public List<JiraSum> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfJiraNumber = 0;
          final int _cursorIndexOfTotal = 1;
          final List<JiraSum> _result = new ArrayList<JiraSum>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final JiraSum _item;
            final String _tmpJiraNumber;
            _tmpJiraNumber = _cursor.getString(_cursorIndexOfJiraNumber);
            final int _tmpTotal;
            _tmpTotal = _cursor.getInt(_cursorIndexOfTotal);
            _item = new JiraSum(_tmpJiraNumber,_tmpTotal);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
