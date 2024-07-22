package com.example.testcomposeui.compose

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.testcomposeui.R
import com.example.testcomposeui.activity.MapActivity
import com.example.testcomposeui.db.CampingSite
import com.example.testcomposeui.ui.theme.TestComposeUITheme
import com.example.testcomposeui.viewmodels.BaseViewModel
import com.example.testcomposeui.viewmodels.MainViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {

    Scaffold(
        topBar = {
            CustomSmallTopAppBar(
                title = "My Camping List",
                onNavigationIconClick = {
                    Log.d(viewModel.TAG, "onNavigationIconClick()")
                }
            )
        }
    ) { paddingValues ->
        // 메인 컨텐츠 영역에 paddingValues를 적용하여 content 컴포저블 호출
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CampDataListView()
        }
    }
}

//공용으로 쓰는 상단 바.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSmallTopAppBar(
    title: String,
    onNavigationIconClick: () -> Unit = {}
) {
    val context = LocalContext.current
    SmallTopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            /* 상단바 배경색. */
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            /* 텍스트 색 */
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menu"
                )
            }
        },
        actions = {
            // 여기에 추가 액션 아이콘을 배치할 수 있습니다.
            IconButton(onClick = {
//                Log.d(TAG, "actions IconButton()")

                gotoMapActivity(context)
            }) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "Map"
                )
            }
        }
    )
}

fun gotoMapActivity(context: Context) {
    val intent = Intent(context, MapActivity::class.java)
    context.startActivity(intent)
}

@Composable
//fun CampDataListView(modifier: Modifier = Modifier, campDatas: List<CampData>) {
fun CampDataListView(modifier: Modifier = Modifier, viewModel: MainViewModel = viewModel()
                     ) {
    Log.d(viewModel.TAG, "CampDataListView()")
    val context = LocalContext.current
    val my_camping_list = viewModel.my_camping_list.observeAsState(initial = emptyList()).value

    FirebaseAuth.getInstance().currentUser?.let { user ->
        val userId = user.uid
        // 이곳에서 Firestore에서 사용자 정보를 가져오는 함수를 호출합니다.
        Log.d(viewModel.TAG, "CampDataListView() userId = $userId")
    } ?: run {
        Log.d(viewModel.TAG, "CampDataListView() userId is null")
        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(my_camping_list) { //users값이 변경될때 블럭이 실행됨.
//        mainViewModel.fetchUsers()
        viewModel.getAllCampingSites { isComplete ->
            Log.d(viewModel.TAG, "getAllCampingSites() isComplete = $isComplete")
            if(!isComplete) {
                //목록 가져오기 실패.
                Toast.makeText(context, "목록 가져오기 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Log.d(viewModel.TAG, "CampDataListView() my_camping_list.size = ${my_camping_list.size}")
    //메모리 관리가 들어간 LazyColumn
    LazyColumn(modifier = modifier.padding(vertical = 14.dp /*상하 패딩.*/)) {
        items(my_camping_list) { my_camping_list ->
            CampDataViewCard(my_camping_list,
                //카드 클릭 이벤트
                onCardClick = { campingSite ->
                    Log.d(viewModel.TAG, "onCardClick() $campingSite")
                    viewModel.selectCampingSite(campingSite)
                    gotoMapActivity(context)
                },
                //
                onCardDeleteClick = { id ->
                    viewModel.deleteCampingSite(id) { isComplete ->
                        if(isComplete) {
                            //삭제 성공.
                            Toast.makeText(context, "삭제 성공", Toast.LENGTH_SHORT).show()
                        } else {
                            //삭제 실패.
                            Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampDataViewCardPreview() {
    TestComposeUITheme {
        CampDataViewCard(capingSite = CampingSite(), onCardClick = {}, onCardDeleteClick = {})
    }
}

@Composable
fun CampDataViewCard(capingSite: CampingSite,
                     onCardClick: (CampingSite) -> Unit,
                     onCardDeleteClick: (String) -> Unit) {
    val typography = MaterialTheme.typography
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp
    )

    Card(
        modifier = Modifier
            .clickable(onClick = { onCardClick(capingSite) }) //카드 클릭 이벤트.
            .fillMaxWidth()     //가로 전체 화면 다쓴다.
            .padding(10.dp),    //카드간 간격.
        shape = RoundedCornerShape(12.dp),
        elevation = elevation   //그림자 영역 지정.
    ) {
        Row(
            /*
            - horizontalArrangement Arrangement = 요소를 어떤식으로 배열할지 설정, Start, End, Center 만 존재.
            -
             */
            modifier = Modifier.padding(10.dp), //패징값.
            verticalAlignment = Alignment.Bottom, //세로 정렬 설정.
            horizontalArrangement = Arrangement.spacedBy(10.dp) //가로 간격 설정.
//            horizontalArrangement = Arrangement.End
        ) {
//            Box(
//                modifier =
//                Modifier
//                    .size(width = 60.dp, height = 60.dp)
//                    .clip(CircleShape)
//                    .background(MyBlue)
//            )
            ProfileImg("https://randomuser.me/api/portraits/women/11.jpg")

            Column {
                Text(
                    text = capingSite.name,
                    style = typography.titleLarge
                )
                Text(
                    text = capingSite.address,
                    style = typography.titleMedium
                )
                Button(onClick = {
                    // 삭제 버튼 클릭 시 처리
//                    Log.d(viewModel.TAG, "CampDataViewCard() onClick() Delete ID = ${capingSite.id}")
                    onCardDeleteClick(capingSite.id)
                }) {
                    Text(text = "삭제")
                }
            }
        }
    }
}

@Composable
fun ProfileImg(imgUrl: String?, modifier: Modifier = Modifier) {
    // 이미지 비트맵
    val bitmap: MutableState<Bitmap?> = remember { mutableStateOf(null) }

    //이미지 모디파이어
    val imageModifier = modifier
        .size(50.dp, 50.dp)
//        .clip(RoundedCornerShape(10.dp)) //이미지 모서리 라운드
        .clip(CircleShape)  //이미지 원형


    //이미지 관리 라이브러리.
    Glide.with(LocalContext.current)
        .asBitmap()
        .load(imgUrl)
        .into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                //bitmap 생성됬을때 호출.
                bitmap.value = resource
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })

    //비트맵이 있다면
    bitmap.value?.asImageBitmap()?.let { fetchedBitmap ->
        Image(
            bitmap = fetchedBitmap,
            contentScale = ContentScale.Fit,
            modifier = imageModifier,
            contentDescription = null
        )
    }
    //이미지가 없다면 기본 아이콘 표시.
        ?: Image(
            painter = painterResource(id = R.drawable.ic_camp_image),
            contentScale = ContentScale.Fit,
            modifier = imageModifier,
            contentDescription = null
        )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TestComposeUITheme {
        MainScreen()
    }
}