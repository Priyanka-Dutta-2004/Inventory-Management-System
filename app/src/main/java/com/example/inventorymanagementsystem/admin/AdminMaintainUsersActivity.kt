package com.example.inventorymanagementsystem.admin

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagementsystem.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.random.Random

class AdminMaintainUsersActivity : AppCompatActivity() {
    private lateinit var searchUserInput: TextInputEditText
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvAdminUsers: TextView
    private lateinit var tvInactiveUsers: TextView
    private lateinit var emptyStateText: TextView
    private lateinit var loadingIndicator: View
    private lateinit var recyclerUsers: RecyclerView
    private lateinit var userAdapter: AdminUsersAdapter

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var latestUsersRequestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_maintain_users)

        setupToolbar()
        bindViews()
        setupRecyclerView()
        setupActions()
        loadDashboardData(showLoader = true)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        searchRunnable?.let(searchHandler::removeCallbacks)
        super.onDestroy()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun bindViews() {
        searchUserInput = findViewById(R.id.searchUserInput)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvAdminUsers = findViewById(R.id.tvAdminUsers)
        tvInactiveUsers = findViewById(R.id.tvInactiveUsers)
        emptyStateText = findViewById(R.id.emptyStateText)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        recyclerUsers = findViewById(R.id.recyclerUsers)
    }

    private fun setupRecyclerView() {
        userAdapter = AdminUsersAdapter(
            onEdit = { user -> showUserDialog(existingUser = user) },
            onToggleStatus = { user -> toggleUserStatus(user) },
            onResetPassword = { user -> showResetPasswordDialog(user) },
        )
        recyclerUsers.layoutManager = LinearLayoutManager(this)
        recyclerUsers.adapter = userAdapter
    }

    private fun setupActions() {
        findViewById<MaterialButton>(R.id.btnAddUser).setOnClickListener {
            showUserDialog()
        }
        findViewById<MaterialButton>(R.id.btnInviteUser).setOnClickListener {
            showUserDialog(isInviteFlow = true)
        }
        searchUserInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let(searchHandler::removeCallbacks)
                searchRunnable = Runnable {
                    loadUsers(query = s?.toString().orEmpty(), showLoader = false)
                }
                searchHandler.postDelayed(searchRunnable!!, 300L)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun loadDashboardData(showLoader: Boolean) {
        loadUsers(query = searchUserInput.text?.toString().orEmpty(), showLoader = showLoader)
        loadSummary()
    }

    private fun loadUsers(query: String, showLoader: Boolean) {
        if (showLoader) {
            loadingIndicator.visibility = View.VISIBLE
        }
        val requestId = ++latestUsersRequestId
        AdminUserApiClient.listUsers(
            query = query.trim(),
            onSuccess = { users ->
                runOnUiThread {
                    if (requestId != latestUsersRequestId || isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    loadingIndicator.visibility = View.GONE
                    userAdapter.submitList(users)
                    emptyStateText.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
                }
            },
            onError = { message ->
                runOnUiThread {
                    if (requestId != latestUsersRequestId || isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    loadingIndicator.visibility = View.GONE
                    emptyStateText.visibility = if (userAdapter.itemCount == 0) View.VISIBLE else View.GONE
                    showToast(message)
                }
            },
        )
    }

    private fun loadSummary() {
        AdminUserApiClient.getSummary(
            onSuccess = { summary ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    tvTotalUsers.text = summary.totalUsers.toString()
                    tvAdminUsers.text = summary.adminUsers.toString()
                    tvInactiveUsers.text = summary.inactiveUsers.toString()
                }
            },
            onError = { message ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    showToast(message)
                }
            },
        )
    }

    private fun showUserDialog(existingUser: AdminUser? = null, isInviteFlow: Boolean = false) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_user_form, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.etName)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.etEmail)
        val passwordLayout = dialogView.findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.etPassword)
        val roleSpinner = dialogView.findViewById<Spinner>(R.id.spinnerRole)
        val statusSpinner = dialogView.findViewById<Spinner>(R.id.spinnerStatus)

        val roleOptions = listOf("ADMIN", "MANAGER", "EMPLOYEE")
        val statusOptions = listOf("ACTIVE", "INACTIVE")
        roleSpinner.adapter = buildSpinnerAdapter(roleOptions)
        statusSpinner.adapter = buildSpinnerAdapter(statusOptions)

        val isEditMode = existingUser != null
        val generatedPassword = if (isInviteFlow) generateTemporaryPassword() else null

        existingUser?.let { user ->
            nameInput.setText(user.name)
            emailInput.setText(user.email)
            roleSpinner.setSelection(roleOptions.indexOf(user.role).coerceAtLeast(0))
            statusSpinner.setSelection(statusOptions.indexOf(user.status).coerceAtLeast(0))
        }

        if (!isEditMode) {
            roleSpinner.setSelection(roleOptions.indexOf("EMPLOYEE"))
        }

        if (isInviteFlow) {
            passwordLayout.visibility = View.GONE
            statusSpinner.setSelection(statusOptions.indexOf("ACTIVE"))
        } else if (isEditMode) {
            passwordLayout.visibility = View.GONE
        }

        val titleRes = when {
            isInviteFlow -> R.string.admin_users_invite_dialog_title
            isEditMode -> R.string.admin_users_edit_dialog_title
            else -> R.string.admin_users_add_dialog_title
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(titleRes)
            .setView(dialogView)
            .setNegativeButton(R.string.admin_users_cancel, null)
            .setPositiveButton(
                if (isEditMode) R.string.admin_users_save_changes else R.string.admin_users_save_user,
                null,
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text?.toString()?.trim().orEmpty()
                val email = emailInput.text?.toString()?.trim().orEmpty()
                val password = passwordInput.text?.toString()?.trim().orEmpty()
                val selectedRole = roleSpinner.selectedItem?.toString().orEmpty()
                val selectedStatus = statusSpinner.selectedItem?.toString().orEmpty()

                if (name.isBlank()) {
                    nameInput.error = getString(R.string.admin_users_error_name_required)
                    return@setOnClickListener
                }
                if (email.isBlank()) {
                    emailInput.error = getString(R.string.admin_users_error_email_required)
                    return@setOnClickListener
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.error = getString(R.string.admin_users_error_email_invalid)
                    return@setOnClickListener
                }
                if (!isEditMode && !isInviteFlow && password.length < 6) {
                    passwordInput.error = getString(R.string.admin_users_error_password_short)
                    return@setOnClickListener
                }

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                if (isEditMode) {
                    updateUser(
                        userId = existingUser.userId,
                        request = AdminUserUpsertRequest(
                            name = name,
                            email = email,
                            role = selectedRole,
                            status = selectedStatus,
                        ),
                        dialog = dialog,
                    )
                } else {
                    createUser(
                        request = AdminUserUpsertRequest(
                            name = name,
                            email = email,
                            password = generatedPassword ?: password,
                            role = selectedRole,
                            status = selectedStatus,
                        ),
                        dialog = dialog,
                        isInviteFlow = isInviteFlow,
                        generatedPassword = generatedPassword,
                    )
                }
            }
        }

        dialog.show()
    }

    private fun createUser(
        request: AdminUserUpsertRequest,
        dialog: androidx.appcompat.app.AlertDialog,
        isInviteFlow: Boolean,
        generatedPassword: String?,
    ) {
        AdminUserApiClient.createUser(
            request = request,
            onSuccess = {
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    dialog.dismiss()
                    loadDashboardData(showLoader = true)
                    val message = if (isInviteFlow && generatedPassword != null) {
                        getString(R.string.admin_users_invite_success, generatedPassword)
                    } else {
                        getString(R.string.admin_users_create_success)
                    }
                    showToast(message)
                }
            },
            onError = { message ->
                runOnUiThread {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    showToast(message)
                }
            },
        )
    }

    private fun updateUser(userId: Long, request: AdminUserUpsertRequest, dialog: androidx.appcompat.app.AlertDialog) {
        AdminUserApiClient.updateUser(
            userId = userId,
            request = request,
            onSuccess = {
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    dialog.dismiss()
                    loadDashboardData(showLoader = true)
                    showToast(getString(R.string.admin_users_update_success))
                }
            },
            onError = { message ->
                runOnUiThread {
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    showToast(message)
                }
            },
        )
    }

    private fun toggleUserStatus(user: AdminUser) {
        val nextStatus = if (user.status.equals("ACTIVE", ignoreCase = true)) "INACTIVE" else "ACTIVE"
        AdminUserApiClient.updateStatus(
            userId = user.userId,
            status = nextStatus,
            onSuccess = {
                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }
                    loadDashboardData(showLoader = true)
                    showToast(getString(R.string.admin_users_status_updated, nextStatus))
                }
            },
            onError = { message ->
                runOnUiThread {
                    showToast(message)
                }
            },
        )
    }

    private fun showResetPasswordDialog(user: AdminUser) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_reset_password, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.admin_users_reset_password_title, user.name))
            .setView(dialogView)
            .setNegativeButton(R.string.admin_users_cancel, null)
            .setPositiveButton(R.string.admin_users_reset_password_action, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newPassword = passwordInput.text?.toString()?.trim().orEmpty()
                if (newPassword.length < 6) {
                    passwordInput.error = getString(R.string.admin_users_error_password_short)
                    return@setOnClickListener
                }

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
                AdminUserApiClient.resetPassword(
                    userId = user.userId,
                    newPassword = newPassword,
                    onSuccess = {
                        runOnUiThread {
                            if (isFinishing || isDestroyed) {
                                return@runOnUiThread
                            }
                            dialog.dismiss()
                            showToast(getString(R.string.admin_users_password_reset_success))
                        }
                    },
                    onError = { message ->
                        runOnUiThread {
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            showToast(message)
                        }
                    },
                )
            }
        }

        dialog.show()
    }

    private fun buildSpinnerAdapter(options: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(this, android.R.layout.simple_spinner_item, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun generateTemporaryPassword(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        return buildString {
            repeat(10) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

private class AdminUsersAdapter(
    private val onEdit: (AdminUser) -> Unit,
    private val onToggleStatus: (AdminUser) -> Unit,
    private val onResetPassword: (AdminUser) -> Unit,
) : RecyclerView.Adapter<AdminUsersAdapter.AdminUserViewHolder>() {
    private val items = mutableListOf<AdminUser>()

    fun submitList(users: List<AdminUser>) {
        items.clear()
        items.addAll(users)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return AdminUserViewHolder(view, onEdit, onToggleStatus, onResetPassword)
    }

    override fun onBindViewHolder(holder: AdminUserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AdminUserViewHolder(
        itemView: View,
        private val onEdit: (AdminUser) -> Unit,
        private val onToggleStatus: (AdminUser) -> Unit,
        private val onResetPassword: (AdminUser) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val chipRole: Chip = itemView.findViewById(R.id.chipRole)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnResetPassword: MaterialButton = itemView.findViewById(R.id.btnResetPassword)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnToggleStatus: MaterialButton = itemView.findViewById(R.id.btnToggleStatus)

        fun bind(user: AdminUser) {
            tvName.text = user.name
            tvEmail.text = user.email
            chipRole.text = user.role
            tvStatus.text = itemView.context.getString(R.string.admin_users_status_format, user.status)

            val isActive = user.status.equals("ACTIVE", ignoreCase = true)
            val statusColor = ContextCompat.getColor(
                itemView.context,
                if (isActive) R.color.success else R.color.warning,
            )
            tvStatus.setTextColor(statusColor)

            val chipBackground = ContextCompat.getColor(
                itemView.context,
                when (user.role.uppercase()) {
                    "ADMIN" -> R.color.surface_light
                    "MANAGER" -> R.color.background_light
                    else -> R.color.surface_white
                },
            )
            chipRole.chipBackgroundColor = android.content.res.ColorStateList.valueOf(chipBackground)
            chipRole.chipStrokeColor = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(itemView.context, R.color.border_soft)
            )
            chipRole.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_dark))

            btnToggleStatus.text = itemView.context.getString(
                if (isActive) R.string.admin_users_disable else R.string.admin_users_enable
            )
            btnToggleStatus.setBackgroundColor(
                ContextCompat.getColor(itemView.context, if (isActive) R.color.warning else R.color.success)
            )
            btnToggleStatus.setTextColor(Color.WHITE)

            btnEdit.setOnClickListener { onEdit(user) }
            btnToggleStatus.setOnClickListener { onToggleStatus(user) }
            btnResetPassword.setOnClickListener { onResetPassword(user) }
        }
    }
}
