package com.finansialku.app.data.repository

import com.finansialku.app.data.dao.UserDao
import com.finansialku.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }

    fun observeUserById(userId: String): Flow<UserEntity?> {
        return userDao.observeUserById(userId)
    }

    suspend fun deleteUser(userId: String) {
        userDao.deleteUser(userId)
    }

    suspend fun deleteAllUsers() {
        userDao.deleteAllUsers()
    }
}
