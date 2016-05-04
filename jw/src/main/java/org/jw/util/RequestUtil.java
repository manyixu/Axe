package org.jw.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.jw.annotation.RequestParam;
import org.jw.bean.FileParam;
import org.jw.bean.FormParam;
import org.jw.bean.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 请求处理 工具类
 * Created by CaiDongYu on 2016/4/26.
 */
public final class RequestUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestUtil.class);
	
	private RequestUtil() {}
	
	/**
	 * 允许接受的字符串种类
	 * 字母、数字、下划线、$
	 */
	private static final String REG_WORD = "A-Za-z0-9_\\$";
	
	/**
	 * 检查action url是否合法
	 * 只支持字母、斜杠、数字、下划线、$符
	 * 以及占位变量符号{}
	 * 占位符花括号不可以紧挨着，否则语意不明
	 */
	public static boolean checkMappingPath(String path){
		do{
			Pattern reg = Pattern.compile("^["+REG_WORD+"\\{\\}/]*$");
			Matcher matcher = reg.matcher(path);
			if(!matcher.find()) return false;
			
			path = castPathParam(path);
			if(path.contains("??")) return false;
		}while(false);
		return true;
	}
	

	/**
	 * 替换pathParam成占位符 ?
	 */
	public static String castPathParam(String nodeName){
		nodeName = nodeName.replaceAll("\\{["+REG_WORD+"]*\\}", "?");
		return nodeName;
	}
	
	/**
	 * 查看 ACTION_MAP中的nodeName，是不是包含占位符的
	 */
	public static boolean containsPathParam(String nodeName){
		return nodeName.contains("?");
	}
	
	/**
	 * 比较请求的nodeName和action_map中带参的pathParamNodeName是否可以匹配
	 */
	public static boolean compareNodeNameAndPathParamNodeName(String nodeName,String pathParamNodeName){
		pathParamNodeName = pathParamNodeName.replaceAll("\\?", "["+REG_WORD+"]*");
		pathParamNodeName = "^"+pathParamNodeName+"$";
		Pattern reg = Pattern.compile(pathParamNodeName);
		Matcher matcher = reg.matcher(nodeName);
		return matcher.find();
	}
	
	/**
	 * 格式化 url 路径
	 * a//b/ 格式化后 /a/b
	 */
	public static String formatUrl(String path){
		path = path.trim().replaceAll("//", "/");
        if(!path.startsWith("/")){
        	path = "/"+path;
        }
        if(path.endsWith("/")){
        	path = path.substring(0, path.length()-1);
        }
        return path;
	}
	
	public static List<FormParam> parseParameter(HttpServletRequest request,String requestPath,String mappingPath){
    	List<FormParam> formParamList = new ArrayList<>();
        //分析url查询字符串
    	Enumeration<String> paramNames = request.getParameterNames();
        while(paramNames.hasMoreElements()){
            String fieldName = paramNames.nextElement();
            String[] fieldValues = request.getParameterValues(fieldName);
            if(ArrayUtil.isNotEmpty(fieldValues)){
                for(String fieldValue:fieldValues){
                    formParamList.add(new FormParam(fieldName,fieldValue));
                }
            }
        }
        //分析url路径参数
        //requestPath 客户端过来的servletPath
        //action方法上的@Quest.value注解值
        Pattern reg4requestPath = Pattern.compile(mappingPath.replaceAll("\\{["+REG_WORD+"]*\\}","(["+REG_WORD+"]*)"));
        Matcher matcher4requestPath = reg4requestPath.matcher(requestPath);
		Pattern reg4mappingPath = Pattern.compile("\\{(["+REG_WORD+"]*)\\}");
		Matcher matcher4mappingPath = reg4mappingPath.matcher(mappingPath);
		boolean fieldValueFind = matcher4requestPath.find();
		for(int fieldValueGroupIndex = 1; matcher4mappingPath.find();fieldValueGroupIndex++){
			String fieldName = matcher4mappingPath.group(1);
			String fieldValue = null;
			if(fieldValueFind && fieldValueGroupIndex<=matcher4requestPath.groupCount()){
				fieldValue = matcher4requestPath.group(fieldValueGroupIndex);
			}
			formParamList.add(new FormParam(fieldName,fieldValue));
		}
		
        return formParamList;
    }

    @SuppressWarnings("unchecked")
	public static Map<String,Object> parsePayload(HttpServletRequest request)throws IOException{
    	Map<String,Object> bodyParamMap = null;
        String body = CodeUtil.decodeURL(StreamUtil.getString(request.getInputStream()));
        if(StringUtil.isNotEmpty(body)){
            try {
            	bodyParamMap = JsonUtil.fromJson(body, Map.class);
            } catch (Exception e){
            	LOGGER.error("read body to json failure,body is: "+body);
            }
        }
        return bodyParamMap;
    }
    
    public static String getRequestMethod(HttpServletRequest request){
    	return request.getMethod().toUpperCase();
    }
    
    public static String getRequestPath(HttpServletRequest request){
    	return request.getServletPath();
    }
    
    

    /**
     * List<?>、List<Object>、List<String>
     * 三个参数形式，都是一样的取请求参数的逻辑
     */
    public static List<?> getParamList(String fieldName,Param param){
    	List<?> parameterValue = null;
    	Map<String,List<FormParam>> fieldMap = param.getFieldMap();
		Map<String,List<FileParam>> fileMap = param.getFileMap();
		if(fieldMap.containsKey(fieldName)){
			List<FormParam> formParamList = fieldMap.get(fieldName);
			List<Object> list = new ArrayList<>();
			for(FormParam formParam:formParamList){
				list.add(formParam.getFieldValue());
			}
			parameterValue = list.size()>0?list:null;
		}else if(fileMap.containsKey(fieldName)){
			parameterValue = fileMap.get(fieldName);
		}
		return parameterValue;
    }
    

    /**
     * List<?>、List<Object>、List<String>
     * 三个参数形式，都是一样的取请求参数的逻辑
     */
    public static List<String> getFormParamList(String fieldName,Map<String,List<FormParam>> fieldMap){
    	List<String> parameterValue = null;
		if(fieldMap.containsKey(fieldName)){
			List<FormParam> formParamList = fieldMap.get(fieldName);
			List<String> list = new ArrayList<>();
			for(FormParam formParam:formParamList){
				list.add(formParam.getFieldValue());
			}
			parameterValue = list.size()>0?list:null;
		}
		return parameterValue;
    }

    /**
     * List<?>、List<Object>、List<String>
     * 三个参数形式，都是一样的取请求参数的逻辑
     */
    public static List<FileParam> getFileParamList(String fieldName,Map<String,List<FileParam>> fileMap){
    	List<FileParam> parameterValue = null;
    	if(fileMap.containsKey(fieldName)){
			parameterValue = fileMap.get(fieldName);
		}
		return parameterValue;
    }

	/**
	 * 检查ActionMethod是否合规
	 * 不准包含基本类型
	 * 基本类型指： int、short、long、double、float、boolean、char等
	 */
	public static void checkActionMethod(Method actionMethod)throws Exception{
    	Parameter[] parameterAry = actionMethod.getParameters();
    	parameterAry = parameterAry == null?new Parameter[0]:parameterAry;
    	for(Parameter parameter:parameterAry){
    		Class<?> parameterType = parameter.getType();
    		Type parameterizedType = parameter.getParameterizedType();
    		//所有的方法参数，都不准是语法糖小写，如int，为了默认值null
			if(parameterType.isPrimitive()){
				throw new Exception(actionMethod.toGenericString()+"#"+parameter+"# is a primitive");
			}
			//如果标注了@RequestParam，需要检测是否是自定义Bean类型
			if(parameter.isAnnotationPresent(RequestParam.class)){
				if(parameterizedType instanceof ParameterizedType){
					Type[] actualTypes = ((ParameterizedType) parameterizedType).getActualTypeArguments();
					for(Type actualType : actualTypes){
						if(!checkActionMethodParamType(actualType)){
							throw new Exception(actionMethod.toGenericString()+"#"+parameter+"# is a customer pojo type");
						}
						//额外判断，不许是List
						if(actualType instanceof Class && ReflectionUtil.compareType(List.class, (Class<?>)actualType)){
							throw new Exception(actionMethod.toGenericString()+"#"+parameter+"# is List<List> type");
						}
					}
				}else if(parameterType.isArray()){
					Class<?> componentType = parameterType.getComponentType();
					if(!checkActionMethodParamType(componentType)){
						throw new Exception(actionMethod.toGenericString()+"#"+parameter+"# is a customer pojo type");
					}
				}else if(parameterizedType instanceof Class){
					if(!checkActionMethodParamType(parameterizedType)){
						throw new Exception(actionMethod.toGenericString()+"#"+parameter+"# is a customer pojo type");
					}
				}
			}
    	}
	}
	
	private static boolean checkActionMethodParamType(Type type){
		do{
			//?
			if(type instanceof WildcardType)break;
			if(type instanceof Class){
				//Object
				if(ReflectionUtil.compareType(Object.class, (Class<?>)type)) break;
				//String
				if(ReflectionUtil.compareType(String.class, (Class<?>)type)) break;
				//FileParam
				if(ReflectionUtil.compareType(FileParam.class, (Class<?>)type)) break;
				//Byte
				if(ReflectionUtil.compareType(Byte.class, (Class<?>)type)) break;
				//Boolean
				if(ReflectionUtil.compareType(Boolean.class, (Class<?>)type)) break;
				//Short
				if(ReflectionUtil.compareType(Short.class, (Class<?>)type)) break;
				//Character
				if(ReflectionUtil.compareType(Character.class, (Class<?>)type)) break;
				//Integer
				if(ReflectionUtil.compareType(Integer.class, (Class<?>)type)) break;
				//Long
				if(ReflectionUtil.compareType(Long.class, (Class<?>)type)) break;
				//Float
				if(ReflectionUtil.compareType(Float.class, (Class<?>)type)) break;
				//Double
				if(ReflectionUtil.compareType(Double.class, (Class<?>)type)) break;
				
				//额外的，允许List
				//其实允许List<list>这样，这样的特殊判断，在这个方法被调用后再额外自行判断
				//List
				if(ReflectionUtil.compareType(List.class, (Class<?>)type)) break;
				
			}
			
			return false;
		}while(false);
		return true;
	}
	
    public static Object getRequestParam(Param param,String fieldName,Class<?> parameterType, Type parameterizedType){
		Object parameterValue = null;
		try {
			if(parameterizedType instanceof ParameterizedType){
				//* 泛型，只支持List这样，但是不支持元素还是泛型了，比如List<Map<String,Object>这样就不支持
				Type[] actualTypes = ((ParameterizedType) parameterizedType).getActualTypeArguments();
				if(ReflectionUtil.compareType(List.class, (Class<?>)((ParameterizedType) parameterizedType).getRawType())){
					if(actualTypes.length > 0){
						Type listParamType = actualTypes[0];
						if(listParamType instanceof WildcardType){
							//List<?>
							parameterValue = RequestUtil.getParamList(fieldName, param);
						}else{
							List<FormParam> formParamList = param.getFieldMap().get(fieldName);
							Class<?> listParamClass = (Class<?>)listParamType;
							if(ReflectionUtil.compareType(Object.class, listParamClass)){
								//List<Object>
								parameterValue = RequestUtil.getParamList(fieldName, param);
							}else if(ReflectionUtil.compareType(String.class, listParamClass)){
								//List<String>
								parameterValue = RequestUtil.getFormParamList(fieldName, param.getFieldMap());
							}else if(ReflectionUtil.compareType(FileParam.class, listParamClass)){
								//List<FileParam>
								parameterValue = RequestUtil.getFileParamList(fieldName, param.getFileMap());
							}else if(ReflectionUtil.compareType(Byte.class, listParamClass)){
								//List<Byte>
								List<Byte> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Byte value =  CastUtil.castByte(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Boolean.class, listParamClass)){
								//List<Boolean>
								List<Boolean> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Boolean value =  CastUtil.castBoolean(formParamList.get(i),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Short.class, listParamClass)){
								//List<Short>
								List<Short> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Short value =  CastUtil.castShort(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Character.class, listParamClass)){
								//List<Character>
								List<Character> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Character value =  CastUtil.castCharacter(formParamList.get(i).getFieldValue());
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Integer.class, listParamClass)){
								//List<Integer>
								List<Integer> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Integer value =  CastUtil.castInteger(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType( Long.class, listParamClass)){
								//List<Long>
								List<Long> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Long value =  CastUtil.castLong(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Float.class, listParamClass)){
								//List<Float>
								List<Float> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Float value =  CastUtil.castFloat(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}else if(ReflectionUtil.compareType(Double.class, listParamClass)){
								//List<double>
								List<Double> list = new ArrayList<>();
								for(int i=0;i<formParamList.size();i++){
									Double value =  CastUtil.castDouble(formParamList.get(i).getFieldValue(),null);
									list.add(value);
								}
								parameterValue = list.size()>0?list:null;
							}
						}
					}
				}
			} else if(parameterType.isArray()){
				List<FormParam> formParamList = param.getFieldMap().get(fieldName);
				if(ReflectionUtil.compareType( parameterType.getComponentType(), Object.class)){
					//Object
					List<?> paramList = RequestUtil.getParamList(fieldName, param);
					parameterValue = CollectionUtil.isNotEmpty(paramList)?paramList.toArray():null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), String.class)){
					//String[]
					List<String> paramList = RequestUtil.getFormParamList(fieldName, param.getFieldMap());
					if(CollectionUtil.isNotEmpty(paramList)){
						String[] paramAry = new String[paramList.size()];
						parameterValue = paramList.toArray(paramAry);
					}
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), FileParam.class)){
					//FileParam[]
					List<FileParam> paramList = RequestUtil.getFileParamList(fieldName, param.getFileMap());
					if(CollectionUtil.isNotEmpty(paramList)){
						FileParam[] paramAry = new FileParam[paramList.size()];
						parameterValue = paramList.toArray(paramAry);
					}
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Byte.class)){
					//Byte[]
					Byte[] list = new Byte[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Byte value =  CastUtil.castByte(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Boolean.class)){
					//Boolean[]
					Boolean[] list = new Boolean[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Boolean value =  CastUtil.castBoolean(formParamList.get(i),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Short.class)){
					//Short[]
					Short[] list = new Short[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Short value =  CastUtil.castShort(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Character.class)){
					//Character[]
					Character[] list = new Character[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Character value =  CastUtil.castCharacter(formParamList.get(i).getFieldValue());
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Integer.class)){
					//Integer[]
					Integer[] list = new Integer[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Integer value =  CastUtil.castInteger(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Long.class)){
					//Long[]
					Long[] list = new Long[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Long value =  CastUtil.castLong(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Float.class)){
					//Float[]
					Float[] list = new Float[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Float value =  CastUtil.castFloat(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}else if(ReflectionUtil.compareType(parameterType.getComponentType(), Double.class)){
					//Double[]
					Double[] list = new Double[formParamList.size()];
					for(int i=0;i<formParamList.size();i++){
						Double value =  CastUtil.castDouble(formParamList.get(i).getFieldValue(),null);
						list[i] = value;
					}
					parameterValue = list.length>0?list:null;
				}
			}else{
				List<FormParam> formParamList = param.getFieldMap().get(fieldName);
				if(ReflectionUtil.compareType(List.class, parameterType)){
					//List
					parameterValue = RequestUtil.getParamList(fieldName, param);
				}else if(ReflectionUtil.compareType(Object.class, parameterType)){
					//Object
					parameterValue = RequestUtil.getParamList(fieldName, param);
				}else if(ReflectionUtil.compareType(String.class, parameterType)){
					//String
					Map<String,List<FormParam>> fieldMap = param.getFieldMap();
					if(fieldMap.containsKey(fieldName)){
						StringBuffer buffer = new StringBuffer("");
						for(int j=0;j<formParamList.size();j++){
							String value =  CastUtil.castString(formParamList.get(j).getFieldValue(), null);
							if(value != null){
								buffer.append(value).append(",");
							}
						}
						parameterValue = buffer.length()>0?buffer.substring(0, buffer.length()-1):null;
					}
				}else if(ReflectionUtil.compareType(parameterType, FileParam.class)){
					//FileParam[]
					List<FileParam> paramList = RequestUtil.getFileParamList(fieldName, param.getFileMap());
					parameterValue = CollectionUtil.isNotEmpty(paramList)?paramList.get(paramList.size()-1):null;
				}else if(ReflectionUtil.compareType(parameterType, Byte.class)){
					//Byte
					for(int i=0;i<formParamList.size();i++){
						Byte value =  CastUtil.castByte(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Boolean.class)){
					//Boolean
					for(int i=0;i<formParamList.size();i++){
						Boolean value =  CastUtil.castBoolean(formParamList.get(i),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Short.class)){
					//Short
					for(int i=0;i<formParamList.size();i++){
						Short value =  CastUtil.castShort(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Character.class)){
					//Character
					for(int i=0;i<formParamList.size();i++){
						Character value =  CastUtil.castCharacter(formParamList.get(i).getFieldValue());
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Integer.class)){
					//Integer
					for(int i=0;i<formParamList.size();i++){
						Integer value =  CastUtil.castInteger(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Long.class)){
					//Long
					for(int i=0;i<formParamList.size();i++){
						Long value =  CastUtil.castLong(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Float.class)){
					//Float
					for(int i=0;i<formParamList.size();i++){
						Float value =  CastUtil.castFloat(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}else if(ReflectionUtil.compareType(parameterType, Double.class)){
					//Double
					for(int i=0;i<formParamList.size();i++){
						Double value =  CastUtil.castDouble(formParamList.get(i).getFieldValue(),null);
						parameterValue = value;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parameterValue;
	}
    
}