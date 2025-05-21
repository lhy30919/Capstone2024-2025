package com.capstone.Algan.adapters

import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.Algan.R
import com.capstone.Algan.models.Message
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

class ChatAdapter(
    private val messageList: MutableList<Message>,
    private val currentUser: String,
    private val forceLeftGravity: Boolean = false
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val storage = FirebaseStorage.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        holder.tvMessageContent.text = message.content ?: ""
        holder.tvMessageTimestamp.text = message.timestamp.toString()
        holder.tvUsername.text = message.username ?: ""

        // Firebase Storage에서 프로필 사진 불러오기 (마이페이지 저장 경로와 동일)
        if (!message.userId.isNullOrEmpty() && !message.companyCode.isNullOrEmpty()) {
            val imageRef = storage.reference.child("profile_images/${message.companyCode}/${message.userId}.jpg")
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(holder.btnProfile.context)
                    .load(uri)
                    .placeholder(R.drawable.baseline_person_2_24)
                    .error(R.drawable.baseline_person_2_24)
                    .into(holder.btnProfile)
            }.addOnFailureListener {
                // 실패 시 기본 이미지 보여줌
                holder.btnProfile.setImageResource(R.drawable.baseline_person_2_24)
            }
        } else {
            holder.btnProfile.setImageResource(R.drawable.baseline_person_2_24)
        }

        if (forceLeftGravity) {
            holder.LinMessageitem.gravity = Gravity.START
            holder.btnDelete.visibility = View.GONE
        } else {
            if (message.username == currentUser) {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnDelete.setOnClickListener {
                    if (messageList.size > position) {
                        messageList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
                holder.LinMessageitem.gravity = Gravity.END
            } else {
                holder.btnDelete.visibility = View.GONE
                holder.LinMessageitem.gravity = Gravity.START
            }
        }

        if (!message.imageUri.isNullOrEmpty()) {
            holder.imageViewMessageImage.visibility = View.VISIBLE
            holder.tvMessageContent.visibility = if (message.content.isNullOrEmpty()) View.GONE else View.VISIBLE
            Glide.with(holder.imageViewMessageImage.context)
                .load(Uri.parse(message.imageUri))
                .into(holder.imageViewMessageImage)
        } else {
            holder.tvMessageContent.visibility = View.VISIBLE
            holder.imageViewMessageImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = messageList.size

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.imageViewMessageImage.context).clear(holder.imageViewMessageImage)
        holder.imageViewMessageImage.setImageDrawable(null)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val tvMessageTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestamp)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val btnProfile: ImageView = itemView.findViewById(R.id.btnProfile)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val imageViewMessageImage: ImageView = itemView.findViewById(R.id.imageViewMessageImage)
        val LinMessageitem: LinearLayout = itemView.findViewById(R.id.LinMessageitem)
    }
}
