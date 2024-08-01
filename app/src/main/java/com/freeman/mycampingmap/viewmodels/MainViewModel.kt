package com.freeman.mycampingmap.viewmodels

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.freeman.mycampingmap.MyApplication
import com.freeman.mycampingmap.auth.FirebaseManager.deleteFirebaseCampingSite
import com.freeman.mycampingmap.auth.FirebaseManager.emailSignUp
import com.freeman.mycampingmap.auth.FirebaseManager.firebaseAuthTokenLogin
import com.freeman.mycampingmap.auth.FirebaseManager.firebaseAuthWithGoogle
import com.freeman.mycampingmap.auth.FirebaseManager.getAllFirebaseCampingSites
import com.freeman.mycampingmap.data.CampingDataUtil
import com.freeman.mycampingmap.db.CampingSite
import com.freeman.mycampingmap.db.LoginType
import com.freeman.mycampingmap.db.User
import com.freeman.mycampingmap.db.UserDao
import com.freeman.mycampingmap.db.UserFactory.createUser
import com.freeman.mycampingmap.utils.MyLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//MVVM 모델 과 컴포스를 적용
class MainViewModel : BaseViewModel() {

    val TAG = this::class.java.simpleName

    //파이어베이스 캠핑장 리스트 정보
    private val _firebaseCampingSites = MutableLiveData<List<CampingSite>>()
    val firebaseCampingSites: LiveData<List<CampingSite>> = _firebaseCampingSites

    private val _dbAllCampingSites = MutableLiveData<List<CampingSite>>()
    val dbAllCampingSites: LiveData<List<CampingSite>> = _dbAllCampingSites

    // 캠핑장 삭제 플레그 - 삭제중에 데이터 싱크가 일어나지 않게 하기 위해.
    var isDeleting: Boolean = false

    fun loginTypeCheckUser(user: User, onComplete: (Boolean, String) -> Unit) {
        MyLog.d(TAG, "loginTypeCheckUser() = $user")

        viewModelScope.launch {
            //db에 저장된 사용자 정보를 이용하여 로그인 신청.
            MyLog.d(TAG, "checkUser() = $user")
            when (user.loginType) {
                LoginType.EMAIL -> { /* 이메일 로그인 */
                    emailLogin(
                        email = user.email,
                        password = user.password,
                        onComplete = onComplete
                    )
                }

                LoginType.GOOGLE -> { /* 구글 로그인 */

                    firebaseAuthWithGoogle(
                        idToken = user.idToken,
                        userDao = userDao,
                        coroutineScope = this
                    ) { success, message ->
                        MyLog.d(TAG, "loginGoogle() firebaseAuthWithGoogle : $success")
                        if (success) onComplete(true, "")
                    }

                }

                LoginType.FACEBOOK -> { /* 페이스북 로그인 */
                }
            }   //when

        }   //viewModelScope.launch
    }

    fun emailRegisterUser(email: String, password: String, onComplete: (Boolean, String) -> Unit) {
        // 파이어베이스 이메일 회원가입.
        MyLog.d(TAG, "emailRegisterUser() = $email, $password")
        val auth = FirebaseAuth.getInstance()
        emailSignUp(email, password) { success, message ->
            if (success) firebaseAuthTokenLogin { success, message ->
                if (success) {
                    auth.currentUser?.let { firebaseUser ->
                        onComplete(success, message ?: "회원 가입 성공")
                    }
                } else {
                    onComplete(success, message ?: "회원 가입 실패")
                }
            } else {
                onComplete(success, message)
            }
        }
    }

//    suspend fun userDaoInsert(firebaseUser: FirebaseUser, email: String, password: String) {
//        // 사용자 정보를 데이터베이스에 저장
//        val newUser = User(
//            uid = firebaseUser.uid,
//            email = email,
//            password = password,
//            username = firebaseUser.displayName ?: ""
//        )
//        userDao.insertUser(newUser)
//    }

    fun syncCampingSites() {
        //db데이터 또는 파이어베이스 사이트 정보 동기화.
        if (isDeleting) return
        _isLoading.value = true
        MyLog.d(TAG, "syncCampingSites()")
        viewModelScope.launch(Dispatchers.IO) {
            val dbAllCampingSiteSelect = async { dbAllCampingSiteSelect() }
            val fierbaseCampingSites = async { getAllFirebaseCampingSites() }
            val localSites = dbAllCampingSiteSelect.await()
            val remoteSites = fierbaseCampingSites.await()
            MyLog.d(TAG, "syncCampingSites() localSites.size : ${localSites.size}")
            MyLog.d(TAG, "syncCampingSites() remoteSites.size : ${remoteSites.size}")
            withContext(Dispatchers.Main) {
                _syncAllCampingList.value = localSites + remoteSites
            }

            val syncCampingSites = CampingDataUtil.syncCampingSites(
                localSites, remoteSites,
                campingSiteRepository, this
            )
            withContext(Dispatchers.Main) {
                MyLog.d(TAG, "syncCampingSites() syncCampingSites.size : ${syncCampingSites.size}")
                _syncAllCampingList.value = syncCampingSites
                _isLoading.value = false
            }
        }
    }

    fun deleteCampingList(id: String) {
        //db 캠핑장 정보 삭제.
        _syncAllCampingList.value?.let { campingSites ->
            val updatedList = campingSites.toMutableList()
            updatedList.removeAll { it.id == id }
            _syncAllCampingList.value = updatedList
        }
    }

    fun deleteCampingSite(campingSite: CampingSite) {
        isDeleting = true
        dbCampingSiteDelete(campingSite) {
            //db 캠핑장 정보 삭제.
            MyLog.d(TAG, "dbCampingSiteDelete() = $it")
            //파이어스토어 데이터베이스에 저장된 캠핑장 정보 삭제.
            deleteFirebaseCampingSite(campingSite) { success ->
                if (success) {
                    deleteCampingList(campingSite.id)
                    Toast.makeText(MyApplication.context, "삭제 성공", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(MyApplication.context, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
                isDeleting = false
            }
        }
    }

}
