package hr.ferit.josipmisic.healthcareapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var buttonLogout: Button
    private lateinit var emailTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var glassCountTextView: TextView
    private lateinit var weightTextView: TextView
    private lateinit var totalCaloriesTextView: TextView
    private lateinit var bpmTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        buttonLogout = view.findViewById(R.id.buttonLogout)
        emailTextView = view.findViewById(R.id.emailTextView)
        usernameTextView = view.findViewById(R.id.usernameTextView)
        bpmTextView = view.findViewById(R.id.bpmTextView)
        glassCountTextView = view.findViewById(R.id.glassCountTextView)
        weightTextView = view.findViewById(R.id.weightTextView)
        totalCaloriesTextView = view.findViewById(R.id.totalCaloriesTextView)



        buttonLogout.setOnClickListener{
            firebaseAuth.signOut()
            val loginIntent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(loginIntent)
            requireActivity().finish()
        }


        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (!currentUserEmail.isNullOrEmpty()) {
            val database = FirebaseDatabase.getInstance().reference
            val korisniciRef = database.child("Korisnici")

            val query = korisniciRef.orderByChild("email").equalTo(currentUserEmail)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                //DataSnapshot predstavlja trenutno stanje podataka na određenom čvoru baze podataka u određenom trenutku
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //snapshot predstavlja jedno dijete (child)
                    for (snapshot in dataSnapshot.children) {
                        val username = snapshot.child("username").value.toString()
                        val email = snapshot.child("email").value.toString()
                        val heartRate = snapshot.child("heartRate").value
                        val displayedHeartRate = heartRate?.toString() ?: "- -"
                        val weight = snapshot.child("weight").value.toString()
                        val glassCount = snapshot.child("glassCount").value.toString()
                        val totalCalories = snapshot.child("totalCalories").value.toString()


                        usernameTextView.text = username
                        emailTextView.text = email
                        bpmTextView.text = displayedHeartRate
                        weightTextView.text = weight
                        glassCountTextView.text = glassCount
                        totalCaloriesTextView.text = totalCalories

                        // Prekini petlju nakon pronalaska odgovarajućeg korisnika
                        break
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Greška prilikom dohvaćanja podataka", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Korisnik nije ulogiran", Toast.LENGTH_SHORT).show()
        }
    }
}
