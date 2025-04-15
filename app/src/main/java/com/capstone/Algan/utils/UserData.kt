package com.capstone.Algan.utils

import android.content.Context
import android.widget.Toast
import com.capstone.Algan.Employee
import com.google.firebase.database.*

object UserData {
    private val database = FirebaseDatabase.getInstance()

    // 🔹 기존 함수 유지 및 리팩토링
    fun fetchAndSaveUserInfo(
        context: Context,
        userId: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val companiesRef = database.getReference("companies")
        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                processUserSnapshot(snapshot, userId, email) { username, companyCode ->
                    if (username != null && companyCode != null) {
                        saveUserInfoToPreferences(context, userId, username, email, companyCode)
                        onSuccess()
                    } else {
                        onFailureWithMessage(context, "사용자 정보를 찾을 수 없습니다.", onFailure)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onFailureWithMessage(context, "사용자 정보 불러오기 실패.", onFailure)
            }
        })
    }

    private fun saveUserInfoToPreferences(
        context: Context,
        userId: String,
        username: String,
        email: String,
        companyCode: String
    ) {
        val sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("userId", userId)
            putString("username", username)
            putString("userEmail", email)
            putString("companyCode", companyCode)
            apply()
        }
    }

    fun isCompanyCodeValid(context: Context, companyCode: String, callback: (Boolean) -> Unit) {
        database.reference.child("companies").child(companyCode).get()
            .addOnSuccessListener { snapshot -> callback(snapshot.exists()) }
            .addOnFailureListener { callback(false) }
    }

    fun loadUserCompanyCode(
        context: Context,
        userId: String,
        callback: (String?) -> Unit
    ) {
        val companiesRef = database.getReference("companies")
        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                processUserSnapshot(snapshot, userId, null) { _, companyCode -> callback(companyCode) }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun loadAllUsersFromCompany(
        context: Context,
        companyCode: String,
        callback: (List<Employee>) -> Unit
    ) {
        val companiesRef = database.getReference("companies").child(companyCode)
        companiesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userList = mutableListOf<Employee>()

                snapshot.child("owner").let {
                    createEmployeeFromSnapshot(it, companyCode)?.let { user -> userList.add(user) }
                }

                snapshot.child("employees").children.forEach {
                    createEmployeeFromSnapshot(it, companyCode)?.let { user -> userList.add(user) }
                }

                callback(userList)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    // 유저 정보 가져오는 공통 함수들
    fun getUserName(companyCode: String, userId: String, callback: (String?) -> Unit) {
        getUserField(companyCode, userId, "username", callback)
    }

    fun getUserPhone(companyCode: String, userId: String, callback: (String?) -> Unit) {
        getUserField(companyCode, userId, "phone", callback)
    }

    fun getUserSalary(companyCode: String, userId: String, callback: (String?) -> Unit) {
        getUserField(companyCode, userId, "salary", callback)
    }

    fun getUserEmail(companyCode: String, userId: String, callback: (String?) -> Unit) {
        getUserField(companyCode, userId, "email", callback)
    }

    fun getUserRole(companyCode: String, userId: String, callback: (String?) -> Unit) {
        getUserField(companyCode, userId, "role", callback)
    }

    fun getCompanyName(companyCode: String, callback: (String?) -> Unit) {
        val companyRef = database.getReference("companies").child(companyCode).child("owner").child("companyName")
        companyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callback(snapshot.getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    // 🔹 공통 로직 처리 함수
    private fun getUserField(companyCode: String, userId: String, field: String, callback: (String?) -> Unit) {
        val companyRef = database.getReference("companies").child(companyCode)

        companyRef.child("owner").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("uid").getValue(String::class.java) == userId) {
                    callback(snapshot.child(field).getValue(String::class.java))
                } else {
                    companyRef.child("employees").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(employeeSnapshot: DataSnapshot) {
                            callback(employeeSnapshot.child(field).getValue(String::class.java))
                        }

                        override fun onCancelled(error: DatabaseError) {
                            callback(null)
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    private fun processUserSnapshot(
        snapshot: DataSnapshot,
        userId: String,
        email: String?,
        callback: (String?, String?) -> Unit
    ) {
        for (companySnapshot in snapshot.children) {
            val ownerSnapshot = companySnapshot.child("owner")
            if (ownerSnapshot.child("uid").getValue(String::class.java) == userId ||
                ownerSnapshot.child("email").getValue(String::class.java) == email
            ) {
                val username = ownerSnapshot.child("username").getValue(String::class.java)
                val companyCode = companySnapshot.key
                callback(username, companyCode)
                return
            }

            companySnapshot.child("employees").children.forEach { employeeSnapshot ->
                if (employeeSnapshot.child("uid").getValue(String::class.java) == userId) {
                    val username = employeeSnapshot.child("username").getValue(String::class.java)
                    val companyCode = companySnapshot.key
                    callback(username, companyCode)
                    return
                }
            }
        }
        callback(null, null)
    }

    fun saveUserData(
        companyCode: String,
        userId: String,
        updatedData: Map<String, String>,
        role: String,
        callback: (Boolean) -> Unit
    ) {
        val userRef = if (role == "사업주") {
            database.getReference("companies").child(companyCode).child("owner")
        } else {
            database.getReference("companies").child(companyCode).child("employees").child(userId)
        }

        userRef.updateChildren(updatedData).addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
    }

    private fun createEmployeeFromSnapshot(snapshot: DataSnapshot, companyCode: String): Employee? {
        val email = snapshot.child("email").getValue(String::class.java) ?: return null
        val username = snapshot.child("username").getValue(String::class.java) ?: "알 수 없음"
        val role = snapshot.child("role").getValue(String::class.java) ?: "알 수 없음"
        val phone = snapshot.child("phone").getValue(String::class.java) ?: "알 수 없음"

        return Employee(
            email = email,
            username = username,
            role = role,
            phone = phone,
            companyCode = companyCode,
            //profileImageUrl = ""
        )
    }

    private fun onFailureWithMessage(context: Context, message: String, onFailure: () -> Unit) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onFailure()
    }
}