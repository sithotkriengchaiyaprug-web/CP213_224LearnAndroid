package com.example.a224_lablearnandroid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FCCardShape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val topCurve = 60f
    moveTo(topCurve, 0f)
    lineTo(width - topCurve, 0f)
    quadraticBezierTo(width, 0f, width, topCurve)
    lineTo(width, height * 0.82f)
    quadraticBezierTo(width, height, width * 0.5f, height)
    quadraticBezierTo(0f, height, 0f, height * 0.82f)
    lineTo(0f, topCurve)
    quadraticBezierTo(0f, 0f, topCurve, 0f)
    close()
}

class RPGCardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var pac by remember { mutableStateOf(120) }
            var sho by remember { mutableStateOf(117) }
            var pas by remember { mutableStateOf(106) }
            var dri by remember { mutableStateOf(116) }
            var def by remember { mutableStateOf(81) }
            var phy by remember { mutableStateOf(115) }
            var hp by remember { mutableStateOf(0.85f) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A1B))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hp)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFFF5252))))
                    )
                    Text("HP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .width(330.dp)
                        .height(500.dp)
                        .shadow(15.dp, FCCardShape)
                        .clip(FCCardShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFF4B5), Color(0xFFD4AF37), Color(0xFFB8860B))
                            )
                        )
                        .border(2.5.dp, Color(0xFF8B6B23), FCCardShape)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1.1f)) {
                            Image(
                                painter = painterResource(id = R.drawable.profile),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { startActivity(Intent(this@RPGCardActivity, ListActivity3::class.java)) },
                                contentScale = ContentScale.FillWidth,
                                alignment = Alignment.TopCenter
                            )

                            Column(
                                modifier = Modifier.padding(top = 40.dp, start = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("CF", fontSize = 60.sp, fontWeight = FontWeight.Black, color = Color.Yellow, lineHeight = 50.sp)
                                Text("116", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Color.Yellow)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(0.95f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BENJAMIN Å EÅ KO",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("PAC", pac, { pac++ }, { pac-- })
                                StatItem("SHO", sho, { sho++ }, { sho-- })
                                StatItem("PAS", pas, { pas++ }, { pas-- })
                                StatItem("DRI", dri, { dri++ }, { dri-- })
                                StatItem("DEF", def, { def++ }, { def-- })
                                StatItem("PHY", phy, { phy++ }, { phy-- })
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 35.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.slovenian_flag),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp, 35.dp).padding(2.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.premierleague_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(45.dp).padding(2.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.manu_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp).padding(2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: Int, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.baseline_arrow_drop_up_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp).clickable { onUp() }
        )
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(value.toString(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Image(
            painter = painterResource(id = R.drawable.outline_arrow_drop_down_24),
            contentDescription = null,
            modifier = Modifier.size(24.dp).clickable { onDown() }
        )
    }
}