package com.freeman.mycampingmap.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM user_table LIMIT 1")
//    fun getUser(): Flow<User?>
    fun getUser(): User?

    @Insert
    fun insertUser(user: User)  //

    @Query("DELETE FROM user_table")
    fun deleteUser()

}