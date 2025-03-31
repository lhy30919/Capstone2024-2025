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
import com.squareup.picasso.Picasso // 이미지 로딩 라이브러리

class ChatAdapter(
    private val messageList: MutableList<Message>,
    private val currentUser: String, // 현재 로그인한 사용자 이름
    private val forceLeftGravity: Boolean = false // 왼쪽 정렬 강제 플래그
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]

        // 메시지 내용 및 사용자 이름 설정
        holder.tvMessageContent.text = message.content ?: "" // null이면 빈 문자열 처리
        holder.tvMessageTimestamp.text = message.timestampAsLong.toString()
        holder.tvUsername.text = message.username ?: ""

        // 프로필 이미지 설정 (Picasso로 이미지 로딩)
        if (!message.profileImageUrl.isNullOrEmpty()) {
            Picasso.get().load(message.profileImageUrl).into(holder.btnProfile)
        } else {
            holder.btnProfile.setImageResource(R.drawable.baseline_person_2_24) // 기본 아이콘
        }

        if (forceLeftGravity) {
            // 왼쪽 정렬 강제
            holder.LinMessageitem.gravity = Gravity.START
            holder.btnDelete.visibility = View.GONE
        } else {
            // 본인 메시지에만 삭제 버튼 표시
            if (message.username == currentUser) {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnDelete.setOnClickListener {
                    if (messageList.size > position) { // 메시지가 리스트에 있을 경우만 삭제
                        messageList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
                // 현재 로그인한 사용자가 작성한 메시지의 레이아웃을 오른쪽 정렬
                holder.LinMessageitem.gravity = Gravity.END
            } else {
                holder.btnDelete.visibility = View.GONE
                // 다른 사용자가 작성한 메시지의 레이아웃을 왼쪽 정렬
                holder.LinMessageitem.gravity = Gravity.START
            }
        }

        // 메시지에 이미지가 있을 경우 표시
        if (!message.imageUri.isNullOrEmpty()) {
            holder.imageViewMessageImage.visibility = View.VISIBLE
            Picasso.get().load(Uri.parse(message.imageUri)).into(holder.imageViewMessageImage)
        } else {
            holder.imageViewMessageImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    // RecyclerView 아이템이 재사용될 때 호출됨
    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        // Picasso에서 이미지 해제
        Picasso.get().cancelRequest(holder.imageViewMessageImage)
        holder.imageViewMessageImage.setImageDrawable(null)
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessageContent: TextView = itemView.findViewById(R.id.tvMessageContent)
        val tvMessageTimestamp: TextView = itemView.findViewById(R.id.tvMessageTimestamp)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val btnProfile: ImageView = itemView.findViewById(R.id.btnProfile)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val imageViewMessageImage: ImageView = itemView.findViewById(R.id.imageViewMessageImage) // 이미지 뷰 추가
        val LinMessageitem: LinearLayout = itemView.findViewById(R.id.LinMessageitem) // LinearLayout 참조 추가
    }
}