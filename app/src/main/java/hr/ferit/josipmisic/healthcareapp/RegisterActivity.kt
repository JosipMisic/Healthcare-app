package hr.ferit.josipmisic.healthcareapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import hr.ferit.josipmisic.healthcareapp.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var showHidePasswordButton: ImageButton
    private lateinit var showHidePasswordButton2: ImageButton
    private lateinit var editTextRegPassword: EditText
    private lateinit var editTextRegConfirmPassword: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        showHidePasswordButton = findViewById(R.id.showHidePasswordButton)
        showHidePasswordButton2 = findViewById(R.id.showHidePasswordButton2)
        editTextRegPassword = findViewById(R.id.editTextRegPassword)
        editTextRegConfirmPassword = findViewById(R.id.editTextRegConfirmPassword)

        showHidePasswordButton.setOnClickListener {
            if ( editTextRegPassword.transformationMethod is PasswordTransformationMethod) {
                // Trenutno je prikazana skrivena lozinka, promijeni način prikaza u običan tekst
                editTextRegPassword.transformationMethod = null
                showHidePasswordButton.setImageResource(R.drawable.ic_password_visibility_on)
            } else {
                // Trenutno je prikazan običan tekst, promijeni način prikaza u skrivenu lozinku
                editTextRegPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                showHidePasswordButton.setImageResource(R.drawable.ic_password_visibility_off)
            }

            // Osvježi tekst kako bi se prikazala nova transformacija, postavljanje kursora na kraj teksta unutar EditText polja,
            // editTextRegPassword.text?.length ?: 0 će vratiti duljinu teksta unutar EditText polja ako tekst nije null, inače će vratiti 0.
            editTextRegPassword.setSelection( editTextRegPassword.text?.length ?: 0)
        }

        showHidePasswordButton2.setOnClickListener {
            if (editTextRegConfirmPassword.transformationMethod is PasswordTransformationMethod) {
                // Trenutno je prikazana skrivena lozinka, promijeni način prikaza u običan tekst
                editTextRegConfirmPassword.transformationMethod = null
                showHidePasswordButton2.setImageResource(R.drawable.ic_password_visibility_on)
            } else {
                // Trenutno je prikazan običan tekst, promijeni način prikaza u skrivenu lozinku
                editTextRegConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                showHidePasswordButton2.setImageResource(R.drawable.ic_password_visibility_off)
            }

            // Osvježi tekst kako bi se prikazala nova transformacija, postavljanje kursora na kraj teksta unutar EditText polja,
            // editTextRegPassword.text?.length ?: 0 će vratiti duljinu teksta unutar EditText polja ako tekst nije null, inače će vratiti 0.
            editTextRegConfirmPassword.setSelection(editTextRegConfirmPassword.text?.length ?: 0)
        }

        binding.buttonRegister.setOnClickListener{
            val username = binding.editTextRegUsername.text.toString()
            val email = binding.editTextRegEmail.text.toString()
            val password = binding.editTextRegPassword.text.toString()
            val confirmPassword = binding.editTextRegConfirmPassword.text.toString()

            val user = User(username, email)

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()){
                if (password == confirmPassword){
                    database = FirebaseDatabase.getInstance().getReference("Korisnici")
                    database.child(username).setValue(user).addOnSuccessListener {
                        Toast.makeText(this,"Uspješno spremljeno", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{
                        Toast.makeText(this,"Greška", Toast.LENGTH_SHORT).show()
                    }
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                        if (it.isSuccessful){
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else{
                    Toast.makeText(this, "Zaporka se ne podudara", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Polja ne mogu biti prazna", Toast.LENGTH_SHORT).show()
            }
        }
        binding.textViewExistingUser.setOnClickListener{
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }
}