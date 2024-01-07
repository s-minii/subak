import { useState } from 'react';
import { Text, View, TouchableOpacity, ScrollView, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import axios from 'axios';
import Icon from 'react-native-vector-icons/Ionicons';
import Config from 'react-native-config';

import { shared } from '../../styles/shared';
import styles from '../../styles/post/newPost';

const NewPost = ({navigation}) => {
  const [showAlert, setShowAlert] = useState(false); // 오류 알림창
  const [alertMessage, setAlertMessage] = useState(''); // 오류 메시지
  const [imageCount, setImageCount] = useState(0); // 이미지 개수

  const [title, setTitle] = useState(''); // 제목
  const [price, setPrice] = useState(0); // 가격
  const [deal, setDeal] = useState('판매하기'); // 거래 방식
  const [content, setContent] = useState(''); // 자세한 설명

  const [noTitle, setNoTitle] = useState(false); // 제목 없음
  const [noContent, setNoContent] = useState(false); // 내용 없음


  return (
    <>
      <ScrollView style={shared.container}>
        <View style={styles.inlineContainer}>
          <TouchableOpacity
            style={shared.iconButton}
            onPress={() => navigation.goBack()}>
            <Icon name="chevron-back" size={30} color="#FFFFFF" />
          </TouchableOpacity>
          <Text style={[shared.text, styles.mainText]}>내 물건 팔기</Text>
        </View>
        
        <View style={styles.content}>
          <TouchableOpacity style={styles.imageContainer}>
            <Icon style={styles.grayText} name="camera" size={25} color="#868b94" />
            <Text style={styles.grayText}>{`${imageCount}/10`}</Text>
          </TouchableOpacity>
          <Text style={styles.inputTag}>제목</Text>
          <TextInput
            style={[
              shared.blankTextInput,
              noTitle && {borderColor: '#dc645b', borderWidth: 1}
            ]}
            onChangeText={text => {
              setTitle(text);
              setNoTitle(false); // 제목이 있다면 경고문구 삭제
            }}
            inputMode="text"
            placeholder="제목"
            placeholderTextColor="#676c74"
          />
          {noTitle && (
            <View style={shared.inlineContainer}>
              <Icon name="alert-circle" size={15} color="#dc645b" />
              <Text style={styles.alertText}> 제목을 적어주세요.</Text>
            </View>
          )}

          <Text style={styles.inputTag}>거래 방식</Text>
          <View style={styles.toggleContainer}>
            <TouchableOpacity 
              style={[styles.toggle, deal === '판매하기' && styles.selectedToggle]}
              onPress={() => setDeal('판매하기')}
            >
              <Text style={[styles.toggleText, deal === '판매하기' && styles.selectedToggleText]}>판매하기</Text>
            </TouchableOpacity>
            <TouchableOpacity 
              style={[styles.toggle, deal === '나눔하기' && styles.selectedToggle]}
              onPress={() => {
                setDeal('나눔하기');
                setPrice(0); // 가격 초기화
              }}
            >
              <Text style={[styles.toggleText, deal === '나눔하기' && styles.selectedToggleText]}>나눔하기</Text>
            </TouchableOpacity>
          </View>
          <TextInput
            style={[shared.blankTextInput, deal === '나눔하기' && shared.textInput]}
            onChangeText={text => setPrice(text)}
            inputMode="numeric"
            keyboardType='numeric'
            placeholder={deal === '나눔하기' ? "₩ 0" : "₩ 가격을 입력해주세요"}
            editable={deal !== '나눔하기'}
            placeholderTextColor="#676c74"
          />

          <Text style={styles.inputTag}>자세한 설명</Text>
          <TextInput
            style={[
              shared.blankTextInput,
              noContent && {borderColor: '#dc645b', borderWidth: 1}
            ]}
            onChangeText={text => {
              setContent(text);
              setNoContent(false); // 내용이 있다면 경고문구 삭제
            }}
            inputMode="text"
            placeholder={"게시글 내용을 작성해 주세요. (판매 금지 물품은 게시가 제한될 수 있어요.)\n\n신뢰할 수 있는 거래를 위해 자세히 적어주세요.\n과학기술정보통신부, 한국 인터넷진흥원과 함께 해요.\n"}
            multiline={true} // 여러 줄의 텍스트
            textAlignVertical="top" // 커서를 위쪽에 배치
            placeholderTextColor="#676c74"
          />
          {noContent && (
            <View style={shared.inlineContainer}>
              <Icon name="alert-circle" size={15} color="#dc645b" />
              <Text style={styles.alertText}> 설명을 적어주세요.</Text>
            </View>
          )}
        </View>
      </ScrollView>
  
      <KeyboardAvoidingView enabled={false}>
        <View style={styles.footer}>
          <TouchableOpacity
            style={[shared.redButton, styles.button]}
            onPress={() => {
              // 제목이 없다면
              !titleCheck(title) && setNoTitle(true);
              // 내용이 없다면
              !contentCheck(content) && setNoContent(true);
              // 제목과 내용이 있다면
              if (titleCheck(title) && contentCheck(content)) { 
                
            axios.post(`http://${Config.DB_IP}/post`, {
              // category: category,
              // postImage: postImage,
              postTitle: title,
              price: price,
            }, {
              timeout: 2000,
            }
            ).then(response => { // 성공 했을 때
              navigation.navigate('FooterTabs');
            })
            .catch(error => { 
                if (error.response) { // 요청은 성공했으나 응답은 실패
                  setAlertMessage(`${error.response.data}`);
                  setShowAlert(true);
                  setTimeout(() => {
                    setShowAlert(false);
                  }, 6000);
                  console.error('Login error.response', error.response.data);
                } else if (error.request) { // timeout으로 요청 실패
                  setAlertMessage('서버와의 연결이 원활하지 않습니다. \n잠시 후 다시 시도해주세요.'); // 오류 메시지
                  setShowAlert(true); // 오류 알림창
                  setTimeout(() => {
                    setShowAlert(false);
                  }, 6000); // 6초 후 알림창 사라짐
                } else { // 기타 오류 발생
                  setAlertMessage(`오류가 발생했습니다. \n[${error.message}]`);
                  setShowAlert(true);
                  setTimeout(() => {
                    setShowAlert(false);
                  }, 6000);
                  console.error('NewPost Unexpected error', error.message);
                }
             }
          )
              }
            }}>
            <Text style={styles.buttonText}>작성 완료</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>

      {showAlert && <Alert message={alertMessage} />}
    </>
  );
};

/**
 * title 내용이 있는 지 체크
 * @param {text} title 
 * @returns 있다면 true, 없다면 false
 */
const titleCheck = (title) => {
  return title === '' ? false : true;
};

/**
 * content 내용이 있는 지 체크
 * @param {text} content 
 * @returns 
 */
const contentCheck = (content) => {
  return content === '' ? false : true;
};

export default NewPost;