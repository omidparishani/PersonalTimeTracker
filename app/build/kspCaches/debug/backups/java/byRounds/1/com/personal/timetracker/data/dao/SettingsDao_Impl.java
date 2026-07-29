package com.personal.timetracker.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.personal.timetracker.data.entity.SettingsEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SettingsDao_Impl implements SettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SettingsEntity> __insertionAdapterOfSettingsEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public SettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSettingsEntity = new EntityInsertionAdapter<SettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `settings` (`id`,`startWorkTime`,`endWorkTime`,`flexibleMinutes`,`minimumWorkMinutes`,`isDarkMode`,`themeColor`,`projects`,`notifEnabled`,`notifMinutesBefore`,`notifTitle`,`notifBody`,`biometricEnabled`,`workLat`,`workLng`,`workRadiusMeters`,`geoAutoCheckIn`,`geoAlertOnly`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getStartWorkTime());
        statement.bindString(3, entity.getEndWorkTime());
        statement.bindLong(4, entity.getFlexibleMinutes());
        statement.bindLong(5, entity.getMinimumWorkMinutes());
        final int _tmp = entity.isDarkMode() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getThemeColor());
        statement.bindString(8, entity.getProjects());
        final int _tmp_1 = entity.getNotifEnabled() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getNotifMinutesBefore());
        statement.bindString(11, entity.getNotifTitle());
        statement.bindString(12, entity.getNotifBody());
        final int _tmp_2 = entity.getBiometricEnabled() ? 1 : 0;
        statement.bindLong(13, _tmp_2);
        statement.bindDouble(14, entity.getWorkLat());
        statement.bindDouble(15, entity.getWorkLng());
        statement.bindDouble(16, entity.getWorkRadiusMeters());
        final int _tmp_3 = entity.getGeoAutoCheckIn() ? 1 : 0;
        statement.bindLong(17, _tmp_3);
        final int _tmp_4 = entity.getGeoAlertOnly() ? 1 : 0;
        statement.bindLong(18, _tmp_4);
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM settings";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final SettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSettingsEntity.insert(settings);
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
  public Flow<SettingsEntity> observe() {
    final String _sql = "SELECT * FROM settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"settings"}, new Callable<SettingsEntity>() {
      @Override
      @Nullable
      public SettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartWorkTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startWorkTime");
          final int _cursorIndexOfEndWorkTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endWorkTime");
          final int _cursorIndexOfFlexibleMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "flexibleMinutes");
          final int _cursorIndexOfMinimumWorkMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "minimumWorkMinutes");
          final int _cursorIndexOfIsDarkMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isDarkMode");
          final int _cursorIndexOfThemeColor = CursorUtil.getColumnIndexOrThrow(_cursor, "themeColor");
          final int _cursorIndexOfProjects = CursorUtil.getColumnIndexOrThrow(_cursor, "projects");
          final int _cursorIndexOfNotifEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notifEnabled");
          final int _cursorIndexOfNotifMinutesBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "notifMinutesBefore");
          final int _cursorIndexOfNotifTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "notifTitle");
          final int _cursorIndexOfNotifBody = CursorUtil.getColumnIndexOrThrow(_cursor, "notifBody");
          final int _cursorIndexOfBiometricEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "biometricEnabled");
          final int _cursorIndexOfWorkLat = CursorUtil.getColumnIndexOrThrow(_cursor, "workLat");
          final int _cursorIndexOfWorkLng = CursorUtil.getColumnIndexOrThrow(_cursor, "workLng");
          final int _cursorIndexOfWorkRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "workRadiusMeters");
          final int _cursorIndexOfGeoAutoCheckIn = CursorUtil.getColumnIndexOrThrow(_cursor, "geoAutoCheckIn");
          final int _cursorIndexOfGeoAlertOnly = CursorUtil.getColumnIndexOrThrow(_cursor, "geoAlertOnly");
          final SettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpStartWorkTime;
            _tmpStartWorkTime = _cursor.getString(_cursorIndexOfStartWorkTime);
            final String _tmpEndWorkTime;
            _tmpEndWorkTime = _cursor.getString(_cursorIndexOfEndWorkTime);
            final int _tmpFlexibleMinutes;
            _tmpFlexibleMinutes = _cursor.getInt(_cursorIndexOfFlexibleMinutes);
            final int _tmpMinimumWorkMinutes;
            _tmpMinimumWorkMinutes = _cursor.getInt(_cursorIndexOfMinimumWorkMinutes);
            final boolean _tmpIsDarkMode;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDarkMode);
            _tmpIsDarkMode = _tmp != 0;
            final int _tmpThemeColor;
            _tmpThemeColor = _cursor.getInt(_cursorIndexOfThemeColor);
            final String _tmpProjects;
            _tmpProjects = _cursor.getString(_cursorIndexOfProjects);
            final boolean _tmpNotifEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifEnabled);
            _tmpNotifEnabled = _tmp_1 != 0;
            final int _tmpNotifMinutesBefore;
            _tmpNotifMinutesBefore = _cursor.getInt(_cursorIndexOfNotifMinutesBefore);
            final String _tmpNotifTitle;
            _tmpNotifTitle = _cursor.getString(_cursorIndexOfNotifTitle);
            final String _tmpNotifBody;
            _tmpNotifBody = _cursor.getString(_cursorIndexOfNotifBody);
            final boolean _tmpBiometricEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBiometricEnabled);
            _tmpBiometricEnabled = _tmp_2 != 0;
            final double _tmpWorkLat;
            _tmpWorkLat = _cursor.getDouble(_cursorIndexOfWorkLat);
            final double _tmpWorkLng;
            _tmpWorkLng = _cursor.getDouble(_cursorIndexOfWorkLng);
            final float _tmpWorkRadiusMeters;
            _tmpWorkRadiusMeters = _cursor.getFloat(_cursorIndexOfWorkRadiusMeters);
            final boolean _tmpGeoAutoCheckIn;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfGeoAutoCheckIn);
            _tmpGeoAutoCheckIn = _tmp_3 != 0;
            final boolean _tmpGeoAlertOnly;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfGeoAlertOnly);
            _tmpGeoAlertOnly = _tmp_4 != 0;
            _result = new SettingsEntity(_tmpId,_tmpStartWorkTime,_tmpEndWorkTime,_tmpFlexibleMinutes,_tmpMinimumWorkMinutes,_tmpIsDarkMode,_tmpThemeColor,_tmpProjects,_tmpNotifEnabled,_tmpNotifMinutesBefore,_tmpNotifTitle,_tmpNotifBody,_tmpBiometricEnabled,_tmpWorkLat,_tmpWorkLng,_tmpWorkRadiusMeters,_tmpGeoAutoCheckIn,_tmpGeoAlertOnly);
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
  public Object get(final Continuation<? super SettingsEntity> $completion) {
    final String _sql = "SELECT * FROM settings WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SettingsEntity>() {
      @Override
      @Nullable
      public SettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartWorkTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startWorkTime");
          final int _cursorIndexOfEndWorkTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endWorkTime");
          final int _cursorIndexOfFlexibleMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "flexibleMinutes");
          final int _cursorIndexOfMinimumWorkMinutes = CursorUtil.getColumnIndexOrThrow(_cursor, "minimumWorkMinutes");
          final int _cursorIndexOfIsDarkMode = CursorUtil.getColumnIndexOrThrow(_cursor, "isDarkMode");
          final int _cursorIndexOfThemeColor = CursorUtil.getColumnIndexOrThrow(_cursor, "themeColor");
          final int _cursorIndexOfProjects = CursorUtil.getColumnIndexOrThrow(_cursor, "projects");
          final int _cursorIndexOfNotifEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "notifEnabled");
          final int _cursorIndexOfNotifMinutesBefore = CursorUtil.getColumnIndexOrThrow(_cursor, "notifMinutesBefore");
          final int _cursorIndexOfNotifTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "notifTitle");
          final int _cursorIndexOfNotifBody = CursorUtil.getColumnIndexOrThrow(_cursor, "notifBody");
          final int _cursorIndexOfBiometricEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "biometricEnabled");
          final int _cursorIndexOfWorkLat = CursorUtil.getColumnIndexOrThrow(_cursor, "workLat");
          final int _cursorIndexOfWorkLng = CursorUtil.getColumnIndexOrThrow(_cursor, "workLng");
          final int _cursorIndexOfWorkRadiusMeters = CursorUtil.getColumnIndexOrThrow(_cursor, "workRadiusMeters");
          final int _cursorIndexOfGeoAutoCheckIn = CursorUtil.getColumnIndexOrThrow(_cursor, "geoAutoCheckIn");
          final int _cursorIndexOfGeoAlertOnly = CursorUtil.getColumnIndexOrThrow(_cursor, "geoAlertOnly");
          final SettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpStartWorkTime;
            _tmpStartWorkTime = _cursor.getString(_cursorIndexOfStartWorkTime);
            final String _tmpEndWorkTime;
            _tmpEndWorkTime = _cursor.getString(_cursorIndexOfEndWorkTime);
            final int _tmpFlexibleMinutes;
            _tmpFlexibleMinutes = _cursor.getInt(_cursorIndexOfFlexibleMinutes);
            final int _tmpMinimumWorkMinutes;
            _tmpMinimumWorkMinutes = _cursor.getInt(_cursorIndexOfMinimumWorkMinutes);
            final boolean _tmpIsDarkMode;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDarkMode);
            _tmpIsDarkMode = _tmp != 0;
            final int _tmpThemeColor;
            _tmpThemeColor = _cursor.getInt(_cursorIndexOfThemeColor);
            final String _tmpProjects;
            _tmpProjects = _cursor.getString(_cursorIndexOfProjects);
            final boolean _tmpNotifEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfNotifEnabled);
            _tmpNotifEnabled = _tmp_1 != 0;
            final int _tmpNotifMinutesBefore;
            _tmpNotifMinutesBefore = _cursor.getInt(_cursorIndexOfNotifMinutesBefore);
            final String _tmpNotifTitle;
            _tmpNotifTitle = _cursor.getString(_cursorIndexOfNotifTitle);
            final String _tmpNotifBody;
            _tmpNotifBody = _cursor.getString(_cursorIndexOfNotifBody);
            final boolean _tmpBiometricEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfBiometricEnabled);
            _tmpBiometricEnabled = _tmp_2 != 0;
            final double _tmpWorkLat;
            _tmpWorkLat = _cursor.getDouble(_cursorIndexOfWorkLat);
            final double _tmpWorkLng;
            _tmpWorkLng = _cursor.getDouble(_cursorIndexOfWorkLng);
            final float _tmpWorkRadiusMeters;
            _tmpWorkRadiusMeters = _cursor.getFloat(_cursorIndexOfWorkRadiusMeters);
            final boolean _tmpGeoAutoCheckIn;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfGeoAutoCheckIn);
            _tmpGeoAutoCheckIn = _tmp_3 != 0;
            final boolean _tmpGeoAlertOnly;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfGeoAlertOnly);
            _tmpGeoAlertOnly = _tmp_4 != 0;
            _result = new SettingsEntity(_tmpId,_tmpStartWorkTime,_tmpEndWorkTime,_tmpFlexibleMinutes,_tmpMinimumWorkMinutes,_tmpIsDarkMode,_tmpThemeColor,_tmpProjects,_tmpNotifEnabled,_tmpNotifMinutesBefore,_tmpNotifTitle,_tmpNotifBody,_tmpBiometricEnabled,_tmpWorkLat,_tmpWorkLng,_tmpWorkRadiusMeters,_tmpGeoAutoCheckIn,_tmpGeoAlertOnly);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
