
var Deck = { 
	id : "",
    slides : [],
    slideIndex : {}
};

var Slide = {
		actions : [],
	 	elements : []
};

var ActionState = {
	
};

var Fonts = [];

var attrConversion = {
    'textAlign': ['text-align',"",""], // Style name, Value preffix, Value Suffix
    'textIndent': ['text-indent',"",''],
    'color': ['color','',''],
    'fontFamily': ['font-family','',''],
    'fontSize': ['font-size','','px'],
    'fontStyle': ['font-style','',''],
    'paddingLeft': ['padding-left','','px'],
    'paddingBottom': ['padding-bottom','','px'],
    'paddingTop': ['padding-top','','px'],
    'paddingRight': ['padding-right','','px'],
    'lineHeight': ['line-height','',''],
    'typographicCase': ['text-transform','',''],
    'trackingLeft': ['letter-spacing','',''],
    'fontWeight': ['font-weight','',''],
    'verticalAlign': ['vertical-align','',''],
    'paragraphStartIndent': ['margin-left','','px'],
    'textDecoration': ['text-decoration','',''],
    'lineThrough': ['text-decoration','',''],
    'ligatureLevel': ['font-variant-ligatures','',''],
    'baselineShift': ['vertical-align','',''],
    'paragraphSpaceBefore': ['padding-top','','px'],
    'paragraphSpaceAfter': ['padding-bottom','','px'],
    'paragraphStartIndent': ['margin-left','','px'],
    'paragraphEndIndent': ['margin-right','','px'],
    'beforeContent': ['--data-before-content','\'','\''],
    'afterContent': ['--data-after-content','\'','\''],
    'whiteSpaceCollapse': ['white-space','','']

};


/*
display: inline-flex;
    align-items: flex-end;
*/
function attrConversor(attrib,nodeLevel) {
   	var attr = attrConversion[attrib.name];
	if(attr){
		var val = attrib.value;
		if(attrib.name == 'fontFamily') {
			Fonts[val]=val;
		}
		else if(attrib.name == 'lineHeight') {
			// val = Number(val.replace("%",""))/100;
		}
		else	if(attrib.name == 'trackingLeft') {
			val = Number(val);
	
		val = val*2;
		if(val < 0)
			val = (Math.abs(val))*-1//Math.ceil(Math.abs(val))*-1;
		else
			val = val;
				
				
		}		
		else	if(attrib.name == 'whiteSpaceCollapse') {
				if(val == 'preserve')
					val = 'pre-line';
		
		}
		else	if(attrib.name == 'ligatureLevel') {
				if(val =="minimum")
					val="no-common-ligatures";
		}		
		else	if(attrib.name == 'lineThrough') {
				if(val =="true")
					val="line-through";
		}		
		else	if(attrib.name == 'baselineShift') {
				if(val =="superscript")
					val="top;line-height: 100%;position: relative;";
				else if(val =="subscript")
					val="sub;line-height:0;font-size: 70%";
		}		
		else	if(attrib.name == 'fontSize') {
			//if don't do it, there is padding at top
		//	return attr[0] + ':' + attr[1] + val + attr[2] +";"+"line-height: 0.75em;";
		
		}
			//return attr[0] + ':' + attr[1] + val + attr[2] +";";
			return [attr[0],(attr[1] + val + attr[2])];
	}
	else{
	
    // console.error(attrib.name);
	
	    return "";
	}
}

var SideTransition = {
	'CubeRotate':'convex-in convex-out',
	'FlipAround':'zoom-in convex-out',
	'PullBack':'concave-in convex-out',
	'None':'none'
}


var leftMarginAPI = 40;
     var firstLineIndentAPI = 10;
var defaultLineHeightApplied = false;
var greaterLetterSpacing = false;
//this lfag is used to track first line
var  isFirstLine = false;
//we are tracking list object at differnet node level
var trackListAtNodelLevel = [];

//fontsize applied at textflow level
var textFlowFontSize = 30;

//we are tracking tabstops of paragrapgh
var tabStops = [];

var paragraphSpaceBeforeTextFlow="";
var paragraphSpaceAfterTextFlow ="";

function traverseXmlDoc(element, xmlDoc,resultXML,nodeLevel) {
    var $xmlDocObj, $xmlDocObjChildren;

    $xmlDocObj = $(xmlDoc);
    $xmlDocObjChildren = $(xmlDoc).children();

	nodeLevel +=1;
    $xmlDocObjChildren.each(function(index, Element) {
        var  $currentObject = $(this),
            // does it have child elements? (if yes, we should call the function recursively)
            childElementCount = Element.childElementCount,
            currentNodeType = $currentObject.prop('nodeType'),
            currentNodeName = $currentObject.prop('nodeName'),
            currentTagName = $currentObject.prop('tagName'),
         	currentTagClass = "";
			
			if(currentNodeName == "listMarkerFormat") {
				
				   return;
			}
			
		
			
			
			
		  var styleAttributes = {};	
			// Loop through all attributes.
		    $.each(this.attributes,function() {
				//ignore paragraphSpaceBefore for Textflow
					if(this.name == "paragraphSpaceBefore" && currentNodeName == "TextFlow"){
						paragraphSpaceBeforeTextFlow = this.value;
					}
					else if(this.name == "paragraphSpaceAfter" && currentNodeName == "TextFlow"){
							paragraphSpaceAfterTextFlow = this.value;
						}
					else{
						 var attrMap = attrConversor(this,nodeLevel) ;

							if(attrMap != ""){

								if(styleAttributes.hasOwnProperty(attrMap[0])){
									styleAttributes[attrMap[0]] += ' ' + attrMap[1];
								}
								else{
									styleAttributes[attrMap[0]] = attrMap[1];
								}

								if(attrMap[0] == "letter-spacing"){
									if(Number(attrMap[1]) > 0)
										greaterLetterSpacing = true;
								}

							}
					}
			
		           
					
		    });
		
		
        	
		
			if(currentTagName == "span"){
				//this is done, as Custom show has
		//		currentTagAtrributes += "position: relative; top:-0.05em;";
				//we put this class, if the span is empty, we need to maintain a blank row height
				//in html row collapses if span is empty. so through css we insert space
				currentTagClass = 'class="TextFlowSpan"';
				
		//		if(currentTagAtrributes.indexOf("font-size")>0 && currentTagAtrributes.indexOf("line-height")<0 && )
						
							
			   /* if(currentTagAtrributes.indexOf("line-height")<0 ){
					if(!defaultLineHeightApplied){
							currentTagAtrributes +="line-height:120%;";
					}
				}
				*/
				if(!styleAttributes.hasOwnProperty("line-height")){
						if(!defaultLineHeightApplied){
								styleAttributes["line-height"] ="120%";
						}
					
				}
				
				if(!styleAttributes.hasOwnProperty("line-height")){
					
								styleAttributes["line-height"] ="120%";
					

				}
					
				if(this.attributes.hasOwnProperty("baselineShift")){
					styleAttributes["top"] = "-0.2em";
						//30 is defautl font size;
						var fs = 30*70/100 ;
						if(!styleAttributes.hasOwnProperty("font-size")){
								styleAttributes["font-size"] ="70%";
						}
						else{
							//var fs = Number(styleAttributes["font-size"].replace("px",""))*70/100 ;
							var fs = Number(styleAttributes["font-size"].replace("px","")) ;
							styleAttributes["font-size"] = fs +"px";
						}
						
						//very special case "," handling
						if($($currentObject[0].firstChild).text() == ","){
							var fs = Number(styleAttributes["font-size"].replace("px",""))*70/100 ;
							styleAttributes["font-size"] = fs +"px";
							styleAttributes["top"] =-1*fs*60/100 + "px";
							
						}
						

				}
				
			}
			
		
			if(currentNodeName == "TextFlow") {
				currentNodeName = "div";
				defaultLineHeightApplied = false;
				
				if(styleAttributes.hasOwnProperty("line-height"))
						defaultLineHeightApplied = true;
					
				
				/* if(currentTagAtrributes.indexOf("line-height")<0){
					

					}
					else{
						defaultLineHeightApplied = true;
					}
					*/
				//we need to put default font size. Custom show default size is 30
			/*	if(currentTagAtrributes.indexOf("font-size")<0)
					currentTagAtrributes += "font-size:30px;"
					*/
					
				
				
				styleAttributes["max-width"] ="PwhPpx";
				styleAttributes["width"] = "PwhPpx";
				styleAttributes["word-wrap"] ="break-word";
				styleAttributes["height"] ="PhtPpx";
				styleAttributes["display"] ="table-cell";
				
				if(!styleAttributes.hasOwnProperty("font-size"))
			   			styleAttributes["font-size"] ="30px";
				if(!styleAttributes.hasOwnProperty("font-variant-ligatures"))
			   			styleAttributes["font-variant-ligatures"] ="common-ligatures";
			
				textFlowFontSize = Number(styleAttributes["font-size"].replace("px",""));
			}
			
			if(currentNodeName == "p"){
				if(isFirstLine){
					//there should be no padding for first line
			   		
						currentTagClass = 'class="TextFlowFirstLine"';
				}
				else{
						
					
				}
				if(paragraphSpaceBeforeTextFlow !=""){
					
						styleAttributes["padding-top"] = paragraphSpaceBeforeTextFlow +"px";
						if(this.attributes.hasOwnProperty("paragraphSpaceBefore")){
							styleAttributes["padding-top"] = this.attributes["paragraphSpaceBefore"].value +"px";
						}
				}
						
							
				 if(paragraphSpaceAfterTextFlow !="")
							styleAttributes["padding-bottom"] = paragraphSpaceAfterTextFlow +"px";
				isFirstLine = false;
				
				if(this.attributes["tabStops"]){
					var tabStopsStr = this.attributes["tabStops"].value.replace(/s/g,'');
					tabStops = tabStopsStr.split(" ");
					
				}
					if(!styleAttributes.hasOwnProperty("line-height")){
						
									styleAttributes["line-height"] ="100%";
						

					}
				
			}
			
			if(currentNodeName == "tab"){
			
			}
			
			
			if(currentNodeName == "list") {
				
					//var list = $.parseJSON(parser.toJson(this));
				
					//resultXML	+=parseList(list.list);
					//return;
					
								currentNodeName = "ul";
								if(this.attributes["listStyleType"]){
										var listClass = this.attributes["listStyleType"].value;
							            var listType;
							            var listId;
							              if(listClass == 'disc' || listClass == 'circle' || listClass == 'square' || listClass == 'check' || listClass =='hyphen' || listClass == 'diamond' || listClass== 'box')
								            {
								              currentNodeName = 'ul';
								            }
								            else
								            {
								              currentNodeName = 'ol';
								            }
								}
								else{
									if(!trackListAtNodelLevel[nodeLevel-1]){
											currentNodeName = 'ul';
											listClass = '';
											styleAttributes["list-style"] ="none";
									}
									else{
										// this is nested continue with parents list stype
										currentNodeName = trackListAtNodelLevel[nodeLevel-1]["listStyle"];
										listClass = '';
										if(currentNodeName == "ul")
											styleAttributes["list-style"] ="none";
									}
								
								}
								
							var listObj = {};
							listObj["listStyle"] = currentNodeName;
							listObj["nestedLevel"] = 1;
							listObj["fontSize"] = 30;
							if(this.attributes["fontSize"])
									listObj["fontSize"] = this.attributes["fontSize"].value;
										
							if(trackListAtNodelLevel[nodeLevel-1]) 
								listObj["nestedLevel"] = trackListAtNodelLevel[nodeLevel-1]["nestedLevel"]+1;
									
							var listStypeType ="decimal";
							switch(listClass){
								case "upperLatin":listStypeType="upper-alpha"; break;
								case "upperRoman":listStypeType="upper-roman"; break;
								case "lowerRoman":listStypeType="lower-roman"; break;
								case "lowerLatin":listStypeType="lower-alpha"; break;
								case "decimal":listStypeType="decimal"; break;
								case "":listStypeType = (listObj["nestedLevel"] % 2 === 0 ?"lower-alpha":"lower-roman"); break;
								
							}								
							

						
						
							listObj["listStyleType"] = listStypeType;
							

							listObj["counter"] = "item"+listObj["nestedLevel"];
							trackListAtNodelLevel[nodeLevel] = listObj;
								
								
							styleAttributes["list-style-type"] =listStypeType;	
						//	styleAttributes["counter-reset"]= listObj["counter"] ;
							
							
							currentTagClass = 'class="'+listClass+'"';
				
							var listFormat =  $currentObject.children("li").find('listMarkerFormat ListMarkerFormat')[0];
				
						
							if(listFormat && listFormat.attributes["counterReset"]){
								
								var counter = listFormat.attributes["counterReset"].value;
								if(listFormat.attributes["counterReset"].value == "ordered") 
									counter = listObj["counter"]+" 1";
								else
									counter = counter.replace("ordered",listObj["counter"]);
							
								styleAttributes["counter-reset"] = counter;
							

							}
								
						styleAttributes["line-height"] ="unset";
						styleAttributes["font-size"] ="unset";	
								
			}
			
			if(currentNodeName == "li"){
				
				styleAttributes["--data-item-type"] =trackListAtNodelLevel[nodeLevel-1]["listStyleType"];
				styleAttributes["--data-item"] =trackListAtNodelLevel[nodeLevel-1]["counter"];
				styleAttributes["counter-increment"] = trackListAtNodelLevel[nodeLevel-1]["counter"];
				
				styleAttributes["display"] = "block";
				
				 var listFormat =  $currentObject.children("listMarkerFormat").find('ListMarkerFormat')[0];
				 var paragraph =  $currentObject.children("p")[0];
				 var spanNode = $(paragraph).find("span")[0];
				 $.each(listFormat.attributes,function() {
				            var attrMap = attrConversor(this,nodeLevel) ;

							if(attrMap != ""){
								if(attrMap[0] != "color" && attrMap[0] != "font-size"){
										if(styleAttributes.hasOwnProperty(attrMap[0])){
											styleAttributes[attrMap[0]] += ' ' + attrMap[1];
										}
										else{
											styleAttributes[attrMap[0]] = attrMap[1];
										}
								}
							
								

							}

				    });
				
					//we take the default size set to list
					var liFontSize = textFlowFontSize;//Number(trackListAtNodelLevel[nodeLevel-1]["fontSize"]);
					
					if(paragraph.attributes["fontSize"])
							liFontSize = Number(paragraph.attributes["fontSize"].value);
							
					if(spanNode.attributes["fontSize"])
								liFontSize = Number(spanNode.attributes["fontSize"].value);

				
				//	styleAttributes["--data-topMargin"] = (textFlowFontSize - Number(trackListAtNodelLevel[nodeLevel-1]["fontSize"]))/2;
				//	styleAttributes["--data-topMargin"] = -(30-30.0*25.0/100);
					
					var bulletFontSize = 30;
				 	styleAttributes["--data-color"] = "";
					if(spanNode.attributes["color"])
							styleAttributes["--data-color"] =spanNode.attributes["color"].value;
					if(paragraph.attributes["color"])
							styleAttributes["--data-color"] =paragraph.attributes["color"].value;
					if(listFormat.attributes["color"])
							styleAttributes["--data-color"] =listFormat.attributes["color"].value;
					if(listFormat.attributes["fontSize"]){
							styleAttributes["--data-fontSize"] =listFormat.attributes["fontSize"].value;
							bulletFontSize = Number(listFormat.attributes["fontSize"].value);
						//	styleAttributes["font-size"] =listFormat.attributes["fontSize"].value +"px";
						//	styleAttributes["--data-topMargin"] = (Number(listFormat.attributes["fontSize"].value)- Number(listFormat.attributes["fontSize"].value)*25.0/100)*-1;
							styleAttributes["--data-nfontSize"] =(Number(listFormat.attributes["fontSize"].value)+5)*-1;
							
						//	liFontSize = Number(listFormat.attributes["fontSize"].value);
						
							}
						else{	
							styleAttributes["--data-nfontSize"] =liFontSize +"px";
						}
						
						
					//	styleAttributes["--data-topMargin"] = (liFontSize - Number(listFormat.attributes["fontSize"].value))/3;
				
					
					var liMargin = 0;
					var paraPadding = 0;
					if(listFormat.attributes["paragraphEndIndent"])
							liMargin = Number(listFormat.attributes["paragraphEndIndent"].value);
				
					if(paragraph.attributes["paddingLeft"])
							paraPadding = Number(paragraph.attributes["paddingLeft"].value);
							
						
					if(paragraphSpaceBeforeTextFlow !=""){

							styleAttributes["--data-space-above"] = paragraphSpaceBeforeTextFlow +"px";
						
					}
					
						if(paragraph.attributes["paragraphSpaceBefore"])
							styleAttributes["--data-space-above"] = paragraph.attributes["paragraphSpaceBefore"].value;
					
						
					var topMargin = 0;
					if(paragraphSpaceBeforeTextFlow !=""){

							styleAttributes["--data-topMargin"] = paragraphSpaceBeforeTextFlow +"px";
							if(paragraph.attributes.hasOwnProperty("paragraphSpaceBefore")){
								//we need to align based bullet as per font size
								//var delta = (textFlowFontSize - liFontSize)*5/100;
								//styleAttributes["--data-topMargin"] = Number(paragraph.attributes["paragraphSpaceBefore"].value)-delta +"px";
								topMargin = Number(paragraph.attributes["paragraphSpaceBefore"].value);
							}
					}
					
						//we need to align based bullet as per font size
						//- (textFlowFontSize - bulletFontSize)
						var delta = (textFlowFontSize - liFontSize)/2;
						styleAttributes["--data-topMargin"] = topMargin-delta ;
				
						
					//100 is width of bullet char
					styleAttributes["--data-left"] =(paraPadding-liMargin-100);
					
					styleAttributes["line-height"] ="100%";
					
			 	//currentTagClass += ' data-left="'+(paraPadding-liMargin)+'" ';
				
			}
	
			
				var currentTagAtrributes = '';
				for (var key in styleAttributes) {
				  if (styleAttributes.hasOwnProperty(key)) {
				   	  var val = styleAttributes[key];
					  currentTagAtrributes += key + ":" + val + ";";
				  }
				}
				
		

			resultXML += "<"+currentNodeName + " "+currentTagClass+" style=\""+currentTagAtrributes+"\">";


        // if it has child nodes, then we call this function recursively
		//Mudassar: we know span is the leaf node, and anyting in it is text
        if ((currentTagName != "span") && childElementCount > 0) {
		
           resultXML = traverseXmlDoc(element,$currentObject,resultXML,nodeLevel);
        }
        else {
	
		/* 	$spanChildren = $currentObject.children();

		   $spanChildren.each(function(ind) {
		        console.error($(this).prop('nodeType') + ":::"+ $(this).prop('nodeName')+ ":::"+ $(this).prop('tagName'));
			});
			*/
			
			var localLetterSpacing  = greaterLetterSpacing;
			if(styleAttributes.hasOwnProperty("letter-spacing")){
				  var val = styleAttributes["letter-spacing"];
					if(Number(val) > 0)
						localLetterSpacing = true;

					else 
						localLetterSpacing = false
				}
			
			dText = "";
			var openTabtag  = false;
			var tabCount  = 0;
			
			var child = $currentObject[0].firstChild;
			while(child){
			
				if(child.nodeName.toLowerCase() == "#text"){
					
					//	if(!openTabtag)
					//		dText += "<span style='display:inline-block;'>";
					
						dText += $(child).text();//+"</span>";
						if(openTabtag)
								dText += "</span>";
								
						if(dText.length > 0 && localLetterSpacing){
								var lastChar = '<span class="noLetterSpacing">'+ dText.substring(dText.length - 1)+'</span>'; 

							   dText = dText.substring(0,dText.length-1) + lastChar;
						}
						openTabtag = false;
				}
				else if(child.nodeName.toLowerCase() == "tab"){
						if(tabCount == 0){
							//below span tag is filler to hold the line, as tabs are absolute below line will be moved up
							dText += "<span style='visibility: hidden;'>a</span>";
						}
						dText += "<span style='position:absolute; left: "+tabStops[tabCount]+"px;'>";
						openTabtag = true;
						tabCount++;
				}
				else if(child.nodeName.toLowerCase() == "br"){
					
						dText += "<br/>";
				}
				else{
					openTabtag = false;
						console.error(child.nodeName.toLowerCase());
				}
			   
			    //do your stuff here
			    child = child.nextSibling;
			}
			
		//	console.error($($currentObject[0].firstChild).text());
         //  	 var dText = $($currentObject[0].firstChild).text();

		//	if(!dText) dText = "";

		
		
			resultXML += dText;
        }
		resultXML += "</"+currentNodeName +">";
    });	
		return resultXML;
}



function isArray(what) {
    return Object.prototype.toString.call(what) === '[object Array]';
}

function getSlideshowData(slideShow){
	var slideshow = {};
	slideshow.id = slideShow.id;
	slideshow.lookupId = slideShow.lookupId;
	if(typeof PRINT_MODE == 'undefined')
		slideshow.url = 'https://'+location.hostname+'/slideshow/'+	slideshow.lookupId;
	else 
		slideshow.url = '';
		
	slideshow.name = slideShow.summary.displayName;	
	slideshow.description = slideShow.summary.description;
	slideshow.owner = slideShow.summary.owner;
	slideshow.company = slideShow.summary.company;
	slideshow.presentationCount = slideShow.summary.presCt;
	slideshow.slideCount = slideShow.summary.slideCt;
	slideshow.thumbnail = MEDIA_CDN_PATH+'thumbnail/'+slideShow.summary.thumbnail.fileName;
	slideshow.parameters = $.parseJSON(parser.toJson(slideShow.parameters)).slide_show_parameters;


	return slideshow;
}

function parseCustomShowXML(xml){
 	// Parse the xml file and get data
  //  var xmlDoc = $.parseXML(xml),
		csJSON = $.parseJSON(parser.toJson(xml,{sanitize:false}));
        $xml = $(xml);
	//	console.error(csJSON.SlideShow.presentations);
	var presentations =  csJSON.SlideShow.presentations.presentation;

		Deck.slideshow = getSlideshowData(csJSON.SlideShow);
	
	if(presentations)
	{
	   
		var presentation =  (isArray(presentations)?presentations[0]:presentations);
		//get presentation id
		Deck.id = presentation.id;
		Deck.name = presentation.name;
		var b = getBackground(presentation);
		Deck.width = Number(b.width);
		Deck.height = Number(b.height);
		var slides = presentation.slides.slideReference;

		if(!isArray(slides)) slides = [slides];
		
		var slideCount = slides.length;
		//loop over each presentation
		for(var i=0; i< slideCount; i++){
			var slideRef = slides[i];
			var slide = slideRef.slide;
		
			
			var slideObj = {};
			//we store all the images used in the slides here
			//this helps us caching the images, before the slide is show
			slideObj.assets = [];
			slideObj.id = slideRef.slide.id;
			slideObj.position = slideRef.position;
			slideObj.name = (slideRef.slide.name == ""?"Slide "+(Number(slideRef.position)+1): slideRef.slide.name) ;
			slideObj.background =getBackground(slide);
			//custom show editor, does not support alpha for Slide
			slideObj.background.fill["color"] = toColor(slideObj.background.fill["color"],1);
			
			slideObj.actions = getSlideActions(slide);
			var slideElements = getSlideElements(slide);
			slideObj.elements = slideElements[0];
			slideObj.images = slideElements[1];
			slideObj.videos = slideElements[2];
			slideObj.elementsById = slideElements[3];
			slideObj.thumbnail = slide.thumbnail.fileName;
			slideObj.returnToPrevious = slide.returnToPrevious;
			
			slideObj.notes = "";
			
			slideObj.transitionIn = "None";
			try{
				if(slideRef.transitionIn && slideRef.transitionIn.effectClass != "")
				slideObj.transitionIn = slideRef.transitionIn.effectClass.split('::')[1];
		  	}
			catch(err){
				
			}
			
			if(SideTransition[slideObj.transitionIn])
				slideObj.transitionIn = SideTransition[slideObj.transitionIn];
			 
			Deck.slides[slideRef.position] = slideObj;
			Deck.slideIndex[slideObj.id] = Number(slideRef.position);
			
		}
		
		Deck.menu = {};
		Deck.menu.info = '<h3>'+Deck.name+'</h3>'+
								'<ul class="slide-menu-items">'+
								    '<li class="menu-item"><img src="'+Deck.slideshow.thumbnail+'" style="width:215px;height:auto"></li>'+
								    '<li class="menu-item">Author:'+Deck.slideshow.owner+'</li>'+
								    '<li class="menu-item">Company:'+Deck.slideshow.company+'</li>'+
								    '<li class="menu-item">URL:'+Deck.slideshow.url+'</li>'+
								    '<li class="menu-item">'+Deck.slideshow.presentationCount+' Presentation</li>'+
								    '<li class="menu-item">'+Deck.slideshow.slideCount+' Slide</li>'+
								    

								'</ul>';
		
		 Deck.menu.share = '<h3>Share</h3>'+
							'<p>To share this SlideShow with someone else, please enter their email address below.</p>'+
							
								  
								    '<p>Email Address <br/><input type="textbox" id="txtEmailAddress" style="width:100%;height:30px;margin-top:10px"><span id="emailError" class="text-danger" style="display:none">Please enter a valid email</span></p>'+
								   
							
								'<p><button onclick="Javascript:validateAndSharePresentation();" style="height: 30px;padding-left: 10px; padding-right: 10px;float: right;margin-bottom: 10px;">Share</button></p>';
		
		Deck.menu.support = '<h3>Send Comment</h3>'+

							    '<p>Type <br/><select id="selType" style="width:100%;margin-top:5px;height:30px">'+
					                  '<option value="enhancement">Enhancement</option>'+
						               '<option value="comment">Comment</option>'+
						               '<option value="bug">Bug</option>'+
					                '</select></p>'+
					
	  
	    '<p>Description <br/> <textarea id="taDescription" rows="5" style="width:100%"></textarea></p>'+

														'<p><button onclick="Javascript:validateAndSendComment();" style="height: 30px;padding-left: 10px; padding-right: 10px;float: right;margin-bottom: 10px;">Share</button></p>';
							
		
	/*	$xml.find('slideShow presentations').each(function () {
            for$(this));
        });
*/
	
	}
    
}


function getSlideElements(slide){
	var elementsArr = [];
	var elementsByIdArr = {};
	var assetsArr = [];
	var videoArr = {};
	if(slide.slideElements && slide.slideElements.slideElement){
			var elements = slide.slideElements.slideElement;
			
			if(!isArray(elements)) elements = [elements];
			
				var elementsCount = elements.length;
				//loop over each elements
				for(var i=0; i< elementsCount; i++){
		
					var element = elements[i];
			
					var elementObj = {};
					elementObj.id = element.id;
					elementObj.name = element.name;
					elementObj.zindex = element.zorder;
					elementObj.interactive = false;
					elementObj.startOnClick = element.startOnClick;
					elementObj.loop = element.loop;
					elementObj.muteAudio = element.muteAudio;
					elementObj.advanceOnComplete = element.advanceOnComplete;
					elementObj.playButtonDisplay = element.playButtonDisplay;
					elementObj.playbackControlsDisplay = element.playbackControlsDisplay;
					
					elementObj.slideElementLink = getSlideElementLink(element);
					
					if(elementObj.startOnClick == "true" || elementObj.slideElementLink.linkEnabled){
								elementObj.interactive = true;
					}
					
					  
				
					elementObj.frame = {};
					elementObj.resourceContent = getResourceContent(element);
					elementObj.chart = getChart(element);
					elementObj.background = getBackground(element);
			
					if(elementObj.background.fill["fillType"] == "None")
						elementObj.background.fill["alpha"] = "0";
				
					elementObj.background.fill["colorHex"] = toColorMatrix2(elementObj.background.fill["color"],elementObj.background.fill["alpha"] );
					
					elementObj.background.fill["color2"] = toColor(elementObj.background.fill["color"],1 );
					elementObj.background.fill["color"] = toColor(elementObj.background.fill["color"],elementObj.background.fill["alpha"] );
			
			
					elementObj.filter = getFilter(element);
					elementObj.renderingInfo = getRenderingInfo(element);
					elementObj.alpha =  elementObj.renderingInfo.alpha;
					if (elementObj.filter && elementObj.filter.dropShadow.enabled == "true")
		            {
		        //        elementObj.alpha = elementObj.filter.dropShadow.alpha;
		            }
			
			
					elementObj.frame.x = elementObj.renderingInfo.x;
					elementObj.frame.y = elementObj.renderingInfo.y;
					elementObj.frame.width = calcUnscaledContentWidth(elementObj);
					elementObj.frame.height = calcUnscaledContentHeight(elementObj);
			
			
					elementObj.frame.scaleX = Number(elementObj.renderingInfo.sx)/100.0;
					elementObj.frame.scaleY = Number(elementObj.renderingInfo.sy)/100.0;
			
					elementObj.frame.rotationAngle = elementObj.renderingInfo.angle;
			
				    elementObj.isMedia = false;
				    elementObj.isVideo = false;
				    elementObj.isImage = false;
				    elementObj.isMedia = false;
				    elementObj.isChart = false;
				    elementObj.isShape = false;
				    elementObj.isPdf = false;
				
					
				 	elementObj.aspectRatio = false;
				
					if (elementObj.resourceContent != null)
				    {
				        elementObj.isMedia = true;
				
						if(elementObj.resourceContent.slideResource.resourceType == "video"){
								elementObj.isVideo = true;
							//	if(elementObj.startOnClick == "true") {
						
							 		elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
									elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
								//	elementObj.frame.scaleX = 1;
								//	elementObj.frame.scaleY = 1;
							//	}
						}
						
						if(elementObj.resourceContent.slideResource.resourceType == "pdf"){
								elementObj.isPdf = true;
								elementObj.interactive = true;
							 	elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
								elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;	
								elementObj.frame.scaleX = 1;
								elementObj.frame.scaleY = 1;
							
						}
			    
					    if(elementObj.resourceContent.slideResource.resourceType == "image"){
								elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
								elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
										elementObj.isImage = true;
				
									}
				    }
				    else if (elementObj.chart != null)
				    {
				        elementObj.isChart = true;
					 	elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
						elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
						elementObj.frame.scaleX = 1;
						elementObj.frame.scaleY = 1;
				    }
				    else if (elementObj.background.shape.type == "Text")
				    {
						elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
						elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
						elementObj.frame.scaleX = 1;
						elementObj.frame.scaleY = 1;
				        elementObj.textInfo = getTextContent(element);
						elementObj.isShape = true;
				    }
					else if (elementObj.background.shape.type == "Circle")
					{
						elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
						elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
						elementObj.frame.scaleX = 1;
						elementObj.frame.scaleY = 1;
				       	elementObj.textInfo = getTextContent(element);
					 	elementObj.isShape = true;
					 	elementObj.aspectRatio = true;
				
					}
					else if (elementObj.background.shape.type == "Ellipse")
					{
						elementObj.frame.width = 	elementObj.frame.width * elementObj.frame.scaleX;
						elementObj.frame.height = 	elementObj.frame.height * elementObj.frame.scaleY;
						elementObj.frame.scaleX = 1;
						elementObj.frame.scaleY = 1;
				        elementObj.textInfo = getTextContent(element);
						elementObj.isShape = true;

					}
					else if (elementObj.background.shape.type == "RoundedRectangle")
				    {
			
						elementObj.frame.scaleX = 1;
						elementObj.frame.scaleY = 1;
				
						elementObj.frame.width = elementObj.renderingInfo.sx;
						elementObj.frame.height = elementObj.renderingInfo.sy;
				        elementObj.textInfo = getTextContent(element);
						elementObj.isShape = true;
				    }
					else if (elementObj.background.shape.type == "Rectangle")
				    {
				
						elementObj.frame.scaleX = "1";
						elementObj.frame.scaleY = "1";

						elementObj.frame.width = elementObj.renderingInfo.sx;
						elementObj.frame.height = elementObj.renderingInfo.sy;
				        elementObj.textInfo = getTextContent(element);
						elementObj.isShape = true;
				    }
					else if (elementObj.background.shape.type == "Triangle")
				    {
			
						elementObj.frame.scaleX = "1";
						elementObj.frame.scaleY = "1";
				
						elementObj.frame.width = elementObj.renderingInfo.sx;
						elementObj.frame.height = elementObj.renderingInfo.sy;
				       	elementObj.textInfo = getTextContent(element);
					 	elementObj.isShape = true;
				    }
				    else if (elementObj.background.shape.type == "Line")
				    {
						elementObj.frame.scaleX = "1";
						elementObj.frame.scaleY = "1";
						
						elementObj.frame.width = Number(elementObj.renderingInfo.sx);
						elementObj.frame.height = Number(elementObj.background.stroke.weight);
						
				//		elementObj.frame.x = Number(elementObj.frame.x) - 15;
				//		elementObj.frame.y = Number(elementObj.frame.y) - 15;
				
					//	elementObj.frame.width = elementObj.renderingInfo.sx;
					//	elementObj.frame.height = elementObj.renderingInfo.sy;
				
					//	elementObj.frame.x = Number(elementObj.frame.x) - Number(elementObj.frame.width);
					//	elementObj.frame.y = Number(elementObj.frame.y) - Number(elementObj.frame.height);
				
				
				        elementObj.isShape = true;
				    }
				    else
				    {
				        elementObj.isShape = true;
				    }
		
					if(elementObj.isMedia ){
				
							if(elementObj.isImage ){
								assetsArr.push(MEDIA_CDN_PATH+'image/'+elementObj.resourceContent.filename);
							}
							else if(elementObj.isVideo){
								assetsArr.push(MEDIA_CDN_PATH+'posterframe/'+elementObj.resourceContent.posterFrame);
								videoArr[elementObj.id] = (MEDIA_CDN_PATH+'video/'+elementObj.resourceContent.filename);
						
							}
				
					}
					elementsArr[element.zorder] = elementObj;
					elementsByIdArr[elementObj.id] = elementObj;
				}
		}
	return [elementsArr,assetsArr,videoArr,elementsByIdArr];
}

function calcUnscaledContentWidth(e)
{
	 var unscaleW = calcUnscaledResourceWidth(e);
	
			
      var padding = 0;
	  if(e.renderingInfo["cropLeft"])
			padding = 	Number(e.renderingInfo.cropLeft) + Number(e.renderingInfo.cropRight);
	
        return (unscaleW - padding);
}

function calcUnscaledContentHeight(e)
{
	 var unscaleH = calcUnscaledResourceHeight(e);
      var padding = 0;
	  if(e.renderingInfo["cropTop"])
			padding = Number(e.renderingInfo.cropTop) + Number(e.renderingInfo.cropBottom);
        return (unscaleH - padding);
}

function calcUnscaledResourceWidth(e)
{
    var val = 0;
    if (e.resourceContent)
    {
        val = e.resourceContent.width;
    }
    else if (e.background)
    {
        val = e.background.width;
    }
   
    return Number(val);
}

function calcUnscaledResourceHeight(e)
{
    var val = 0;
    if (e.resourceContent)
    {
        val = e.resourceContent.height;
    }
    else if (e.background)
    {
        val = e.background.height;
    }
   
    return Number(val);
}


function getSlideElementLink(e){
	var linkEle = {};
	linkEle["linkEnabled"] = false;
	linkEle["tooltipEnabled"] = false;
	if(e.slideElementLink && e.slideElementLink.linkEnabled == "true"){
		linkEle["linkEnabled"] = true;
		linkEle["linkTarget"] = e.slideElementLink.linkTarget;
		linkEle["linkType"] = e.slideElementLink.linkType;
		linkEle["tooltipEnabled"] = (e.slideElementLink.tooltipEnabled == "true"?true:false);
		if(linkEle["tooltipEnabled"]){
			
			 	linkEle["tooltipText"] = e.slideElementLink.tooltipText;
		}
			 
	}
	
	return linkEle;
}



function getResourceContent(e){
	
	if(e.resourceContent){
		var resourceContent = {};
		resourceContent.posterFrame = e.resourceContent.defaultPosterFrame?e.resourceContent.defaultPosterFrame.fileName:"";
		resourceContent.firstFrame = e.resourceContent.firstFrame?e.resourceContent.firstFrame.fileName:"";
		resourceContent.filename = e.resourceContent.filename;
		resourceContent.width = e.resourceContent.width;
		resourceContent.height = e.resourceContent.height;
		resourceContent.id = e.resourceContent.id;
		resourceContent.slideResource = {};
		resourceContent.slideResource.name = e.resourceContent.slideResource.name;
		resourceContent.slideResource.resourceType = e.resourceContent.slideResource.resourceType;
		resourceContent.externalUrl = e.resourceContent.externalUrl;
		resourceContent.extension = resourceContent.filename.substr(resourceContent.filename.lastIndexOf("."));
		return resourceContent;
	}
	return null;
}

function getTextContent(e){

	var textXML = e.text.content;
	 textXMLDoc = $.parseXML(textXML);
        $textXMLDoc = $(textXMLDoc);
		greaterLetterSpacing = false;
		isFirstLine = true;
		paragraphSpaceBeforeTextFlow = "";
		paragraphSpaceAfterTextFlow = "";
	var htmlXML = traverseXmlDoc(e,$textXMLDoc,"",-1);
	

	return htmlXML;

}



function getChart(e){
	if(e.chart){
		var chart = {};
		chart.options = $.parseJSON(parser.toJson(e.chart.options));
		chart.data = $.parseJSON(parser.toJson(e.chart.data));
	
		return chart;
	}
	return null;
}

function getBackground(e){
	var backJson = $.parseJSON(parser.toJson(e.background.parameters));
	var strokeAlpha = 1;
	if(backJson.background.stroke["strokeType"] == "None") strokeAlpha = 0;
	
	backJson.background.stroke["color"] = toColor(backJson.background.stroke["color"],strokeAlpha );
	return backJson.background;
}

function getFilter(e){
	var backJson = $.parseJSON(parser.toJson(e.filter.parameters));
	backJson.filters.dropShadow["color"] = toColor(backJson.filters.dropShadow["color"],backJson.filters.dropShadow.alpha );
	backJson.filters.dropShadow["angle"] = toRadians(backJson.filters.dropShadow["angle"],1 );


	return backJson.filters;
}

function getRenderingInfo(e){
	var backJson = $.parseJSON(parser.toJson(e.renderingInfo.parameters));
	var alpha = Number(backJson.renderingInfo.alpha);
	backJson.renderingInfo.alpha = alpha > 1 ? (alpha / 100) : (alpha);

	return backJson.renderingInfo;
}


/******** actions *****/

function getSlideActions(slide){
	var actionArr = {};
	
	if(slide.slideElementActions && slide.slideElementActions.slideElementAction){
		var actions = slide.slideElementActions.slideElementAction;
		
		if(!isArray(actions)) actions = [actions];
			
		var actionCount = actions.length;
		
		//loop over each actions
		for(var i=0; i< actionCount; i++){
		
			var action = actions[i];
			var actionObj = {};
			actionObj.id = action.id;
			actionObj.params = getActionState(action);
			actionObj.position = Number(action.position);
			actionObj.actionClass = action.actionClass.split('::')[1];
			actionObj.elementId = action.slideElement;
			actionObj.slideId = action.slide;
			actionObj.transition = action.transition;
			actionObj.type = action.type;
			
			if(!actionArr[actionObj.elementId]) actionArr[actionObj.elementId] = [];
			
			actionArr[actionObj.elementId].push(actionObj);
			
		}
	}
	return actionArr;
}

function getActionState(e){
	var actionStateJson = $.parseJSON(parser.toJson(e.parameter));
	actionStateJson.actionState.params.delay = Number(actionStateJson.actionState.params.delay)/1000.0;
	actionStateJson.actionState.params.duration = Number(actionStateJson.actionState.params.duration)/1000.0;
	return actionStateJson.actionState.params;
}

function toColor(num,alpha) {
    num >>>= 0;
    var b = num & 0xFF,
        g = (num & 0xFF00) >>> 8,
        r = (num & 0xFF0000) >>> 16,
        a = ( (num & 0xFF000000) >>> 24 ) / 255 ;
//Mudassar: a is alwaus coming 0;
//'#'+num.toString(16);//
    return "rgba(" + [r, g, b, alpha].join(",") + ")";
}

function toRadians (angle) {
  return angle * (Math.PI / 180);
}

function toDegrees (angle) {
  return angle * (180 / Math.PI);
}

function toColorMatrix(num,alpha) {
    num >>>= 0;
    var b = num & 0xFF,
        g = (num & 0xFF00) >>> 8,
        r = (num & 0xFF0000) >>> 16,
        a = ( (num & 0xFF000000) >>> 24 ) / 255 ;
//Mudassar: a is alwaus coming 0;
//'#'+num.toString(16);//
	var mat =''+Number(r)/255.0+' 0 0 0 0 '+
			 '0 '+Number(g)/255.0+' 0 0 0 '+
			 '0 0 '+Number(b)/255.0+' 0 0 '+
			 '0 0 0 1 0';
   var mat =  '0 0 0 0 '+Number(r)/255.0+
			 ' 0 0 0 0 '+Number(g)/255.0+
             ' 0 0 0 0 '+Number(b)/255.0+
             ' 0 0 0 '+alpha+' 0';
    return mat;
}
function toColorMatrix2(num,alpha) {
	//http://alistapart.com/article/finessing-fecolormatrix
    num >>>= 0;
    var b = num & 0xFF,
        g = (num & 0xFF00) >>> 8,
        r = (num & 0xFF0000) >>> 16,
        a = ( (num & 0xFF000000) >>> 24 ) / 255 ;
//Mudassar: a is alwaus coming 0;
//'#'+num.toString(16);//
	var mat =''+(Number(r)/255.0 + Number(alpha))+' 0 0 0 0 '+
			 '0 '+(Number(g)/255.0 + Number(alpha))+' 0 0 0 '+
			 '0 0 '+(Number(b)/255.0 + Number(alpha))+' 0 0 '+
			 '0 0 0 1 0';
  
    return mat;
}

function isMobileEnabled (){
  var check = false;
  if( navigator.userAgent.match(/iPad/i) != null)
    return true;
    
   
  (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4)))check = true})(navigator.userAgent||navigator.vendor||window.opera);
  (function(a){if(/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4)))check = true})(navigator.userAgent||navigator.vendor||window.opera);
  return check;
}

function isSafari(){
	var ua = navigator.userAgent.toLowerCase(); 
	if (ua.indexOf('safari') != -1) { 
	  if (ua.indexOf('chrome') > -1) {
	    return false;
	  } else {
	   return true;
	  }
	}
	return false;
}

function get_browser_info(){
	if(typeof PRINT_MODE == 'undefined'){
  	  var ua=navigator.userAgent,tem,M=ua.match(/(opera|chrome|safari|firefox|msie|trident(?=\/))\/?\s*(\d+)/i) || []; 
	    if(/trident/i.test(M[1])){
	        tem=/\brv[ :]+(\d+)/g.exec(ua) || []; 
	        return {name:'IE ',version:(tem[1]||'')};
	        }   
	    if(M[1]==='Chrome'){
	        tem=ua.match(/\bOPR\/(\d+)/)
	        if(tem!=null)   {return {name:'Opera', version:tem[1]};}
	        }   
	    M=M[2]? [M[1], M[2]]: [navigator.appName, navigator.appVersion, '-?'];
	    if((tem=ua.match(/version\/(\d+)/i))!=null) {M.splice(1,1,tem[1]);}
	    return {
	      name: M[0],
	      version: M[1]
	    };
	}
	else{
		  return {
		      name: 'Chrome',
		      version: '55'
		    };
	}
 }

/**
 * detect IE
 * returns version of IE or false, if browser is not Internet Explorer
 */
function detectIE() {
  var ua = window.navigator.userAgent;

  // Test values; Uncomment to check result …

  // IE 10
  // ua = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)';
  
  // IE 11
  // ua = 'Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko';
  
  // Edge 12 (Spartan)
  // ua = 'Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.71 Safari/537.36 Edge/12.0';
  
  // Edge 13
  // ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586';

  var msie = ua.indexOf('MSIE ');
  if (msie > 0) {
    // IE 10 or older => return version number
    return parseInt(ua.substring(msie + 5, ua.indexOf('.', msie)), 10);
  }

  var trident = ua.indexOf('Trident/');
  if (trident > 0) {
    // IE 11 => return version number
    var rv = ua.indexOf('rv:');
    return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
  }

  var edge = ua.indexOf('Edge/');
  if (edge > 0) {
    // Edge (IE 12+) => return version number
    return parseInt(ua.substring(edge + 5, ua.indexOf('.', edge)), 10);
  }

  // other browser
  return false;
}

function toColorHex(num){
	num >>>= 0;
    var b = num & 0xFF,
        g = (num & 0xFF00) >>> 8,
        r = (num & 0xFF0000) >>> 16,
        a = ( (num & 0xFF000000) >>> 24 ) / 255 ;

   return "#"+fullColorHex(r,g,b);
}

var fullColorHex = function(r,g,b) {   
  var red = rgbToHex(r);
  var green = rgbToHex(g);
  var blue = rgbToHex(b);
  return red+green+blue;
};

function isNumeric(n) {
  return !isNaN(parseFloat(n)) && isFinite(n);
}

var rgbToHex = function (rgb) { 
  var hex = Number(rgb).toString(16);
  if (hex.length < 2) {
       hex = "0" + hex;
  }
  return hex;
};

var isWebkit = function(){
	return (navigator.userAgent.search(/WebKit/) > -1);
}


var registerCustomCharts = function(){
		var ShadowLineElement = Chart.elements.Line.extend({
		  draw:function () {


		     var ctx = this._chart.chart.ctx;
			
		    var originalStroke = ctx.stroke;

		    ctx.stroke = function () {
		      ctx.save()
		      ctx.shadowColor = "#AEAEAE"
		      ctx.shadowBlur = 4
		      ctx.shadowOffsetX = 0
		      ctx.shadowOffsetY = 4
		      originalStroke.apply(this, arguments)
		      ctx.restore()
		    }

		    Chart.elements.Line.prototype.draw.apply(this, arguments)

		    ctx.stroke = originalStroke;
		  }
		})

		Chart.defaults.ShadowLine = Chart.defaults.line;
		Chart.controllers.ShadowLine = Chart.controllers.line.extend({
		  datasetElementType: ShadowLineElement
		})
		
		var ShadowBarElement = Chart.elements.Rectangle.extend({
		      draw:function  () {
		        ctx = this._chart.ctx;
		        var originalStroke = ctx.stroke;
		
		         this._chart.ctx.shadowColor = "#A0A0A0"
		          this._chart.ctx.shadowBlur = 2
		          this._chart.ctx.shadowOffsetX = 1
		          this._chart.ctx.shadowOffsetY = 2
		        Chart.elements.Rectangle.prototype.draw.apply(this, arguments);
		        this._chart.ctx.stroke = originalStroke;
		      }
		    });
		    Chart.defaults.ShadowBar = Chart.defaults.bar
		    Chart.controllers.ShadowBar = Chart.controllers.bar.extend({
		      dataElementType: ShadowBarElement
		    })
		
		
		 var ShadowHorizontalBarElement = Chart.elements.Rectangle.extend({
		      draw:function () {
		        ctx = this._chart.ctx;
		        var originalStroke = ctx.stroke;
		         this._chart.ctx.shadowColor = "#A0A0A0"
		          this._chart.ctx.shadowBlur = 4
		          this._chart.ctx.shadowOffsetX = 2
		          this._chart.ctx.shadowOffsetY = 2
		        Chart.elements.Rectangle.prototype.draw.apply(this, arguments);
		        this._chart.ctx.stroke = originalStroke;
		      }
		    });
		    Chart.defaults.ShadowHorizontalBar = Chart.defaults.horizontalBar
		    Chart.controllers.ShadowHorizontalBar = Chart.controllers.horizontalBar.extend({
		      dataElementType: ShadowHorizontalBarElement
		    })
		
		
		 var ShadowArcElement = Chart.elements.Arc.extend({
		      draw:function () {
		        ctx = this._chart.ctx;
		        this._view.borderWidth=0;
		         this._chart.ctx.shadowColor = "#ffffff";
		          this._chart.ctx.shadowBlur = 0;
		          this._chart.ctx.shadowOffsetX = 4;
		          this._chart.ctx.shadowOffsetY = -4;
		        Chart.elements.Arc.prototype.draw.apply(this, arguments);
		        this._chart.ctx.shadowColor = "#00ffff";
		          this._chart.ctx.shadowBlur = 0;
		          this._chart.ctx.shadowOffsetX = 0;
		          this._chart.ctx.shadowOffsetY = 0;
		      }
		    });
		    Chart.defaults.ShadowPie = Chart.defaults.pie
		    Chart.controllers.ShadowPie = Chart.controllers.pie.extend({
		      dataElementType: ShadowArcElement
		    })
}



var ActionState = {
	"BUILD_IN": "BuildIn",
    "BUILD_OUT": "BuildOut",
    "BUILD_ACTION": "Action",
    "TRANSITION_ON_CLICK": "OnClick",
    "TRANSITION_ON_DELAY": "OnDelay",
    "TRANSITION_WITH_PREVIOUS": "WithPrev",
    "STATUS_PENDING": "statusPending",
    "STATUS_RUNNING": "statusRunning",
    "STATUS_COMPLETE": "statusComplete"
};


var ActionStateInstance = function(actionState,elementId) { 
		this.raw = actionState;
		this.actionType = actionState.type;
		this.transitionType = actionState.transition;
		
		this.target = $('#'+actionState.elementId);
		this.dependants = [];
		this.groupActions = {};
		this.startDelay = 0;
		this.elementId = elementId;
		this.totalTime = 0;
	};

ActionStateInstance.prototype.raw = "";
//this is used to identify fragment type 
ActionStateInstance.prototype.extraClass = "";
ActionStateInstance.prototype.elementId = "";
ActionStateInstance.prototype.actionId = "";
//identifies action dependant group
ActionStateInstance.prototype.actionGroup = "";
ActionStateInstance.prototype.dependants = [];
//all the actions grouped by clicks
ActionStateInstance.prototype.groupActions = {};
ActionStateInstance.prototype.target = ""; //this is the html element 
ActionStateInstance.prototype.actionType = "";
ActionStateInstance.prototype.transitionType = "";
ActionStateInstance.prototype.target = "";
ActionStateInstance.prototype.startDelay = 0;
ActionStateInstance.prototype.totalTime = 0;
ActionStateInstance.prototype.easing = "";




ActionStateInstance.prototype.prepTarget = function() { 

	if(this.actionType != ActionState.BUILD_IN)
		this.target.css("visibility",'visible');
	else
		this.target.css("visibility",'hidden');

};
	

	







	
	







	


var ActionStateManager = function() { 

	};

ActionStateManager.prototype.states = [];
ActionStateManager.prototype.elementActions = [];




ActionStateManager.prototype.reset = function(slide) { 
	this.elementActions = [];
	slide.clickActions = [];
	
	this.loadStates(slide);
	
	//Mudassar: we are reverse sorting, so that we can put the depenants with right order
//	this.states.sort().reverse();
	
	//we reverse and start reading the actions in reverse
	//if it has transition with previous, then we club it as dependants
	//
    var actionsDependantOnPrevious = null;


	//here we are identifying dependants and calculating the totaltime for the action with dependant
	for(var i=this.states.length-1; i > -1; i--){
		
		var actionStateInstance = this.states[i];
		
		if(!actionStateInstance) continue;
	//	 actionStateInstance.prepTarget();
		actionStateInstance.totalTime = actionStateInstance.raw.params.delay + actionStateInstance.raw.params.duration;
		
		 if (actionStateInstance.transitionType == ActionState.TRANSITION_WITH_PREVIOUS)
           {
               actionsDependantOnPrevious = actionsDependantOnPrevious ? (actionsDependantOnPrevious) : ([]);
               actionsDependantOnPrevious.push(actionStateInstance);
			 
               continue;
           }
         if (actionsDependantOnPrevious)
           {
               actionStateInstance.dependants = actionsDependantOnPrevious;
		
               actionsDependantOnPrevious = null;
           }
		
		//Mudassar : Earlier I had missed to set the intial delay. So here we set the 
		//delay in start of animation
		actionStateInstance.startDelay =  actionStateInstance.raw.params.delay ;
		
			
	}
	
//	this.states.sort();
	
	var trackedTime = 0;
	var lastActionId = "";
	var lastActionStateInstance = "";
	var lastTransitionType = "";
	var TIME_BUFFER = 0.2; //time between to actions
	//here we are identifying each individual action/clubed action and their start time

	
	for(var i=0; i< this.states.length; i++){
		

		var actionStateInstance = this.states[i];
		if(!actionStateInstance) continue;
			
		this.setAnimationAttributes(actionStateInstance,slide);
		
	 	if (actionStateInstance.transitionType != ActionState.TRANSITION_WITH_PREVIOUS &&
			actionStateInstance.transitionType != ActionState.TRANSITION_ON_CLICK)
           {
				trackedTime +=TIME_BUFFER;
				if(lastActionId != ''){
					
					actionStateInstance.actionGroup = lastActionId;
					
						if(!lastActionStateInstance.groupActions[actionStateInstance.elementId])
							 lastActionStateInstance.groupActions[actionStateInstance.elementId] = [];
						lastActionStateInstance.groupActions[actionStateInstance.elementId].push(actionStateInstance);
					
					
					actionStateInstance.extraClass += ' click-dependant-action ';
				}	
				actionStateInstance.extraClass += this.getVisibility(actionStateInstance);
				
			 	actionStateInstance.startDelay = trackedTime + actionStateInstance.raw.params.delay ;
				var maxActionExecutionTime = actionStateInstance.totalTime;
				if(actionStateInstance.dependants.length > 0) {
					//we need to see for parllel anims...if there is any dependancy
						for(var j=0; j< actionStateInstance.dependants.length; j++) {
							var dependant = actionStateInstance.dependants[j];
								dependant.startDelay = actionStateInstance.startDelay + dependant.raw.params.delay;
								if(dependant.totalTime > maxActionExecutionTime)
									maxActionExecutionTime = dependant.totalTime;
						}
		
				}
				trackedTime = trackedTime + maxActionExecutionTime;
           }
			//reset track time, as build is changed
		 if(actionStateInstance.transitionType == ActionState.TRANSITION_WITH_PREVIOUS){
				if(lastActionId != ''){
					
					actionStateInstance.actionGroup = lastActionId;
					
					if(!lastActionStateInstance.groupActions[actionStateInstance.elementId])
						 lastActionStateInstance.groupActions[actionStateInstance.elementId] = [];
					lastActionStateInstance.groupActions[actionStateInstance.elementId].push(actionStateInstance);
					
					actionStateInstance.extraClass += ' click-dependant-action ';
					actionStateInstance.extraClass += this.getVisibility(actionStateInstance);
				}
			}
			//reset track time, as build is changed
		 if(actionStateInstance.transitionType == ActionState.TRANSITION_ON_CLICK){
			trackedTime = 0;
			var maxActionExecutionTime = actionStateInstance.totalTime;
			actionStateInstance.actionId = actionStateInstance.raw.id;
			lastActionStateInstance = actionStateInstance;
			 lastActionId = actionStateInstance.actionId;
			actionStateInstance.extraClass = " click-action ";
			actionStateInstance.extraClass += this.getVisibility(actionStateInstance);
			trackedTime = trackedTime + maxActionExecutionTime;
			
			slide.clickActions.push(actionStateInstance);
		}
		
	
	 }
	
	
	
	for(var i=0; i< this.states.length; i++){
		
		var actionStateInstance = this.states[i];
		
		if(!actionStateInstance) continue;
		
		//Store all actions which are not in click, click acctions are tracked seperately
		//also we are skipping grouped actions, as they will be added dynamically
		if(actionStateInstance.transitionType != ActionState.TRANSITION_ON_CLICK && actionStateInstance.actionGroup == "")
			this.elementActions[actionStateInstance.raw.elementId] = actionStateInstance;
		
	}


};

ActionStateManager.prototype.setAnimationAttributes = function(actionStateInstance,slide) {
	var animClass ="";
	var easing = "";
	var actionStyle = "";
	var browser=get_browser_info();
	
	var target = slide.elementsById[actionStateInstance.elementId];
	
	if(actionStateInstance.raw.actionClass == "ZoomAction"){
			
			if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
					animClass = "zoomIn";

			if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
					animClass = "zoomOut";

		
	}
	else if(actionStateInstance.raw.actionClass == "MoveAction"){
	
			
		
		$.each(actionStateInstance.raw.params.userOption, function(i, option) {
		    if(option.variable == "_direction"){
				if(option.value == "leftToRight"){
					if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
								animClass = "slideInLeft";
								actionStyle +="--slideInLeft:"+-1*(Deck.width + Number(target.frame.width))+ "px;";
					}

					if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
								animClass = "slideOutRight";
								actionStyle +="--slideOutRight:"+(Deck.width +Number(target.frame.width))+ "px;";
					}
										
				}
				else if(option.value == "rightToLeft"){
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "slideInRight";
										actionStyle +="--slideInRight:"+(Deck.width +Number(target.frame.width))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "slideOutLeft";
										actionStyle +="--slideOutLeft:"+-1*(Deck.width +Number(target.frame.width))+ "px;";
							}
						
				}
				else if(option.value == "topToBottom"){
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "slideInDown";
										actionStyle +="--slideInDown:"+-1*(Deck.height +Number(target.frame.height))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "slideOutUp";
										actionStyle +="--slideOutUp:"+(Deck.height +Number(target.frame.height))+ "px;";
							}
				}
				else if(option.value == "bottomToTop"){
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "slideInUp";
										actionStyle +="--slideInUp:"+(Deck.height +Number(target.frame.height))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "slideOutDown";
										actionStyle +="--slideOutDown:"+-1*(Deck.height +Number(target.frame.height))+ "px;";
							}
				}
			}
			if(option.variable == "_easing"){
				easing = option.value.toLowerCase();
			}
			
		});
		
		
	}
	else if(actionStateInstance.raw.actionClass == "FadeAction"){
	
		if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
				animClass = "fadeIn";
		
		if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
				animClass = "fadeOut";
		
		$.each(actionStateInstance.raw.params.userOption, function(i, option) {
		    if(option.variable == "_alphaFrom"){
				actionStyle +="opacity:" +option.value+";";
			
			}else if(option.variable == "_alphaTo"){
			
			}
			if(option.variable == "_easing"){
				easing = option.value.toLowerCase();
			}
			
		});
	}
	else if(actionStateInstance.raw.actionClass == "BlurAction"){
		
			if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
					animClass = "blurIn";

			if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
					animClass = "blurOut";
	//	actionStyle +="opacity:0;";
		$.each(actionStateInstance.raw.params.userOption, function(i, option) {
		    if(option.variable == "_direction"){
						if(option.value == "horizontal"){
								if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
										animClass = "blurHIn";

								if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
										animClass = "blurHOut";
						}
				
						if(option.value == "vertical"){
								if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
										animClass = "blurVIn";

								if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
										animClass = "blurVOut";
						}
			}
			if(option.variable == "_easing"){
				easing = option.value.toLowerCase();
			}

		});
	}
	else if(actionStateInstance.raw.actionClass == "WipeAction"){
			$.each(actionStateInstance.raw.params.userOption, function(i, option) {
				if(actionStateInstance.raw.type == ActionState.BUILD_IN &&
						browser.name.toLowerCase() == "chrome" && 
							browser.version == "66"){
								//fix for CSHTML-247
								//special handling for chrom version 66
									animClass = "fadeIn";
									actionStyle +="opacity:0.1;";
				}
				else if(option.variable == "_direction"){
					if(option.value == "Left"){
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "wipeInLeft";
										actionStyle +="--wipeInLeft:"+(Number(target.frame.width))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "wipeOutRight";
										actionStyle +="--wipeOutRight:"+(Number(target.frame.width))+ "px;";
							}
					}
					else if(option.value == "Right"){
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "wipeInRight";
										actionStyle +="--wipeInRight:"+(Number(target.frame.width))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "wipeOutLeft";
										actionStyle +="--wipeOutLeft:"+(Number(target.frame.width))+ "px;";
							}
					}
					else if(option.value == "Down"){
						
							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "wipeInDown";
										actionStyle +="--wipeInDown:"+(Number(target.frame.height))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "wipeOutUp";
										actionStyle +="--wipeOutUp:"+(Number(target.frame.height))+ "px;";
							}
					}
					else if(option.value == "Up"){
							animClass = "wipeInUp";

							if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
										animClass = "wipeInUp";
										actionStyle +="--wipeInUp:"+(Number(target.frame.height))+ "px;";
							}

							if(actionStateInstance.raw.type == ActionState.BUILD_OUT){
										animClass = "wipeOutDown";
										actionStyle +="--wipeOutDown:"+(Number(target.frame.height))+ "px;";
							}
					}
				}
				if(option.variable == "_easing"){
					easing = option.value.toLowerCase();
				}
			});
	}
	else{
			if(actionStateInstance.raw.type == ActionState.BUILD_IN) 
					animClass = "fadeIn";

			if(actionStateInstance.raw.type == ActionState.BUILD_OUT) 
					animClass = "fadeOut";

			$.each(actionStateInstance.raw.params.userOption, function(i, option) {
			    if(option.variable == "_alphaFrom"){
					actionStyle +="opacity:" +option.value+";";

				}else if(option.variable == "_alphaTo"){

				}
				if(option.variable == "_easing"){
					easing = option.value.toLowerCase();
				}

			});
	}
	
	switch(easing){
		case "linear": easing = "linear"; break;
		case "bounce": easing = "cubic-bezier(.8,0,.03,1)"; break;
		case "elastic": easing="cubic-bezier(0, 1.4, 1, 1)"; break;
		case "exponential": easing="cubic-bezier(0.795, 0.000, 0.005, 1.000)"; break;
	}
	//cubic-bezier(0.760, 0.090, 0.785, 0.085)
	
/*	var easingStyle = "-webkit-animation-timing-function:"+easing+";animation-timing-function:"+easing+";";
	var result = [];
	var actionId = (actionStateInstance.actionId != ""? 'data-action-id="'+actionStateInstance.actionId+'" ':'');
	var actionGroup = (actionStateInstance.actionGroup != ""? 'data-action="'+actionStateInstance.actionGroup+'" ':'');
	result['animate'] =  actionId + actionGroup + 'data-delay="'+actionStateInstance.startDelay+'"  data-duration="'+actionStateInstance.raw.params.duration+'" data-animate="'+animClass+'"';
	result['style'] = easingStyle+actionStyle;
	*/
	
	actionStateInstance.easing = easing;
	actionStateInstance.animateClass = animClass;
	actionStateInstance.animateData = actionStyle;
	
	
}
	

ActionStateManager.prototype.getVisibility = function(actionStateInstance) {

	/*	if(actionStateInstance.raw.type == ActionState.BUILD_OUT) {
			return 	" build-out-action ";
		}
		if(actionStateInstance.raw.type == ActionState.BUILD_IN) {
			return 	" build-in-action ";
		}
		*/


        return "";
}
	




ActionStateManager.prototype.loadStates = function(slide) {

		
		//this emptys the array
		this.states = [];
		
		for (var elementId in slide.actions) {
		    // check if the property/key is defined in the object itself, not in parent
		    if (slide.actions.hasOwnProperty(elementId)) {        
			
				    var eleActions = slide.actions[elementId];
					
			   		//get all actions for a given element
					for(var i=0; i< eleActions.length; i++){
						var actionState = eleActions[i];

						var actionStateInstance = new ActionStateInstance(actionState,elementId);

					    this.states[actionState.position] = actionStateInstance;
					}
		 
				
		    }
		}
		
	


}





	





	


var SlideBuilder = function(actionMgr) { 
	this.actionStateManager = actionMgr;
	};

SlideBuilder.prototype.LineEndType = {
		"END_CIRCLE": "circle",
	    "END_DIAMOND": "diamond",
	    "END_SQUARE": "square",
	    "ARROW_LINE": "line",
	    "ARROW_SOLID": "solid",
	    "ARROW_COMPLEX": "complex",
		"END_NONE": "none"
	};
SlideBuilder.prototype.LineType = {
		"LINE_TYPE_SOLID": "Solid",
	    "LINE_TYPE_DASHED_1": "Dashed-1",
	    "LINE_TYPE_DASHED_2": "Dashed-2",
	    "LINE_TYPE_DASHED_3": "Dashed-3"
	};


SlideBuilder.prototype.StrokeType = {
		"STROKE_TYPE_NONE": "None",
	    "STROKE_TYPE_LINE": "Line"
	};
SlideBuilder.prototype.actionStateManager = "";	
SlideBuilder.prototype.currentSlide = "";
//used in printing
SlideBuilder.prototype.fixedVerticalPosition = 0;

SlideBuilder.prototype.filters = function(){
	return '<svg version="1.1" style="height:0px"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">'+
		'<defs>'+
			'<filter id="bothBlur" width="150%" height="150%" x="-25%" y="-25%" color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="20,20" />'+
			'</filter>'+
			'<filter id="hBlur" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="20,1" />'+
			'</filter>'+
			'<filter id="vBlur" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="1,20"  >'+
			'	<!--	<animate attributeName="stdDeviation" from="0,0" to="0,50" dur="2s"/>-->'+

				'</feGaussianBlur>'+
			'</filter>'+
			'<filter id="bothBlurOut" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="70,70" />'+
			'</filter>'+
			'<filter id="hBlurOut" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="30,1" />'+
			'</filter>'+
			'<filter id="vBlurOut" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="1,20" />'+
			'</filter>'+
			'<filter id="noBlur" width="150%" height="150%" x="-25%" y="-25%"  color-interpolation-filters="sRGB">'+
				'<feGaussianBlur in="SourceGraphic" stdDeviation="0,0" />'+
			'</filter>'+
		'</defs>'+
	'</svg>';
}

SlideBuilder.prototype.create = function() {

		var skeleton = '';
		var sectionTag = "div";
		
		if(typeof PRINT_MODE == 'undefined')
			sectionTag = "section";

			var THUMBNAIL_PATH = MEDIA_CDN_PATH+"thumbnail/";
			for (var i=0; i< Deck.slides.length; i++) {
			    var slide = Deck.slides[i];
				slide.info = new SlideInfo(slide);
			    var slideBackground = 'width: '+slide.background.width+'px; height: '+slide.background.height+'px;';
				var thumbnail = THUMBNAIL_PATH + slide.thumbnail;
				 skeleton += 		'<'+sectionTag+' id="'+i+'"  data-transition="'+slide.transitionIn+'" class=" slideContainer zoomifier-slide-'+i+'" style="'+slideBackground+';top:'+this.fixedVerticalPosition+'px" data-state="zoomifier-slide-'+i+'" data-menu-title="'+thumbnail+'">';
		//	skeleton += '<div style="'+slideBackground+'">';
		
				if(typeof PRINT_MODE == 'undefined')
					skeleton += '<div class="slide-loading"></div>';
				this.actionStateManager.reset(slide);
				
					slide.animationElements = "";
					slide.backgroundElements = '<div style="margin:0px 0px 0px 0px;width:calc(100% - 0px);height:calc(100% - 0px);background-color:'+slide.background.fill.color +';"></div>';
					for (var j=0; j< slide.elements.length; j++) {
					    	var element = slide.elements[j];
					
						/*	if(!slide.actions[element.id])
									skeleton +=	this.renderElement(element,slide);
							else
								slide.animationElements +=	this.renderElement(element,slide);
								*/
						if(!slide.actions[element.id])
							slide.backgroundElements +=	this.renderElement(element,slide);
						else
							slide.animationElements +=	this.renderElement(element,slide);
					
					
					}
					
				if(typeof PRINT_MODE == 'undefined'){
						skeleton +='<div id="navLeft'+i+'"  style="pointer-events:auto;position:absolute;left:0px;top:0px;width:33%; height:100%;opacity(0.5);" onclick="Javascript:prev();"></div>';
						skeleton +='<div id="navRight'+i+'"  style="pointer-events:auto;position:absolute;right:0px;top:0px;width:77%; height:100%;opacity(0.5);" onclick="Javascript:next();"></div>';
						skeleton +='<div id="backgroundLayer'+i+'"  style="pointer-events:none;position:absolute;left:0px;top:0px;width:100%; height:100%;"></div>';

						skeleton +='<div id="animLayer'+i+'"  style="pointer-events: none;position:absolute;left:0px;top:0px;width:100%; height:100%;opacity(0.5)"></div>';
				}
				else{
					skeleton += slide.backgroundElements;
					skeleton += slide.animationElements;
				}
					if(typeof PRINT_MODE == 'undefined')
						this.fixedVerticalPosition = 0
					else
						this.fixedVerticalPosition = Number(this.fixedVerticalPosition) + Number(slide.background.height);
			
		//		skeleton += 		'</div>';
				 skeleton += 		'</'+sectionTag+'>';
			}

	 	 
		
		return skeleton + this.filters();

}




SlideBuilder.prototype.renderElement = function(element,slide){
	var xml = "";
	var rotation = "";
	var transformStyle = "";
	if(element.frame.rotationAngle != "")
			rotation = "rotate("+element.frame.rotationAngle+"rad)";
			//for line rotation we need origin as 0,0
     var transformOriginClass = ((element.isShape && element.background.shape.type == "Line")?"transformContainer":"");

	 var actionClass = "";
	 var animAttributes = "";
	var actionStyle = "";
	var visiblity = "";
	var opacity = '';

	
	this.currentSlide = slide;
	
		opacity = 'opacity:'+element.alpha+';';
	
	var actionStateInstance  = this.actionStateManager.elementActions[element.id];
	if(typeof PRINT_MODE == 'undefined'){
		  if(actionStateInstance ){
				actionClass = actionStateInstance.extraClass;



				animAttributes = (actionStateInstance.actionId != ""? 'data-action-id="'+actionStateInstance.actionId+'" ':'')+
								(actionStateInstance.actionGroup != ""? 'data-action="'+actionStateInstance.actionGroup+'" ':'') +
								'data-delay="'+actionStateInstance.startDelay+
								'"  data-duration="'+actionStateInstance.raw.params.duration+
								'" data-animate="'+actionStateInstance.animateClass+'"';
				actionStyle = "-webkit-animation-timing-function:"+actionStateInstance.easing+";animation-timing-function:"+actionStateInstance.easing+";" +
						actionStateInstance.animateData	;

				if(actionStateInstance.raw.actionClass == "BlurAction")
					opacity = '--zalpha:'+element.alpha+';opacity:'+element.alpha+';'

				if(actionStateInstance.raw.actionClass == "FadeAction")
					opacity = '--zalpha:'+element.alpha+';opacity:'+element.alpha+';'
			}
			else{
				//we are checking if this elements is animated on click
				if(slide.actions[element.id]){
					//if yes, then apply the state of first action. There could be a chance of both BUILD_IN and BUILD_OUT
					//gettting applied on same element
					 var eaction = slide.actions[element.id][0];
						if(eaction.type == ActionState.BUILD_OUT) {
							actionClass = 	" build-out-action ";
						}
						if(eaction.type == ActionState.BUILD_IN) {
							actionClass = 	" build-in-action ";
						}
				}
			}
	}
	

	var playpauseClickHandler = "";
	
	if(element.isVideo && element.startOnClick == "true") 
			playpauseClickHandler = 'onclick="Javascript:play(this);"';
			
	var frameWidth = element.frame.width;		
	var frameHeight = element.frame.height;
	
	if(element.isVideo || element.isImage){
		//cropping is supported only for image and video
		//we need to set transform origin for cropped elements
		//so we push the origin to centre of visible area
		if(element.renderingInfo.cropEnabled == "true"){
			
			var w = Number(element.resourceContent.width)-Number(element.renderingInfo.cropRight) -Number(element.renderingInfo.cropLeft);
			var h = Number(element.resourceContent.height)-Number(element.renderingInfo.cropBottom)-Number(element.renderingInfo.cropTop);
			var scaleVal = Number(element.frame.scaleX);
			var originX = (Number(element.renderingInfo.cropLeft) + w/2)*Number(element.frame.scaleX);
			var originY = (Number(element.renderingInfo.cropTop) + h/2)*Number(element.frame.scaleX);
			
			transformStyle = "transform-origin:"+originX + "px "+originY+"px;";
		}
		frameWidth = element.resourceContent.width*Number(element.frame.scaleX);		
		frameHeight = element.resourceContent.height*Number(element.frame.scaleX);
	}
	 var elementVerticalPos = Number(this.fixedVerticalPosition) + Number(element.frame.y);
	
	 xml = '<div  id="'+element.id+'" class="componentContainer '+transformOriginClass+ actionClass+' " '+animAttributes+'  style="--myrotate:'+rotation+';left: '+element.frame.x+'px; top: '+elementVerticalPos+'px;-webkit-transform: '+rotation+'; -moz-transform: '+rotation+'; transform: '+rotation+'; width: '+frameWidth+'px; height: '+frameHeight+'px;z-index: '+element.zindex+';'+opacity+actionStyle+transformStyle+visiblity+(element.interactive?"pointer-events:auto;":"pointer-events:none")+'" '+playpauseClickHandler+'>';

	var scaleLayer = '<div class="transformContainer" style="-webkit-transform: scale('+element.frame.scaleX+', '+element.frame.scaleY+');-moz-transform: scale('+element.frame.scaleX+', '+element.frame.scaleY+');transform: scale('+element.frame.scaleX+', '+element.frame.scaleY+')" >';
	
	
	xml +=scaleLayer;
	
	if(element.slideElementLink.linkEnabled && (typeof PRINT_MODE == 'undefined'))
		xml += this.createHyperlink(element);
	
		
	if(element.isVideo){
		xml +=this.renderVideoElement(element);
	}
	if(element.isPdf){
		xml +=this.renderPDFViewer(element);
	}
	else if(element.isImage){
		xml += this.renderImageElement(element);
	}
	else if(element.isChart){
		xml += this.renderChartElement(element);
	}
	else if(element.isShape){
		if(element.background.shape.type == "Text")
			xml += this.renderTextElement(element);
		else
			xml += this.renderShapeElement(element);
	}
	
	if(element.slideElementLink.linkEnabled && (typeof PRINT_MODE == 'undefined'))
			xml += '</a>';

	   xml +=   '</div>'+
			'</div>';

	return xml;


}

SlideBuilder.prototype.createHyperlink = function(element){
	var tooltip = '';
	var href = "#";
	var target = "";
	if(element.slideElementLink.linkType == "SLIDE"){
			if(typeof PRINT_MODE == 'undefined'){
					href = 'Javascript:goToSlide('+Deck.slideIndex[element.slideElementLink.linkTarget]+')';
					
					
			}
			else
			{
					href = '#page='+(Number(Deck.slideIndex[element.slideElementLink.linkTarget])+1);
			}
			
	
			if(element.slideElementLink.tooltipEnabled){
				if(element.slideElementLink.tooltipText == undefined)
				 {	
					if(Deck.slideIndex[element.slideElementLink.linkTarget])
					   tooltip = 'title="Click to go to \''+(Deck.slides[Deck.slideIndex[element.slideElementLink.linkTarget]].name)+'\'"';
				}
				else
					tooltip = 'title="'+element.slideElementLink.tooltipText+'"';
				
			}
	
	}
	else if(element.slideElementLink.linkType == "POSITION"){
			if(element.slideElementLink.linkTarget == "NEXT") {
			
					if(typeof PRINT_MODE == 'undefined'){
								href = 'Javascript:next()';
					}
					else
					{
							href = '#page=1';
					}
					if(element.slideElementLink.tooltipEnabled){
						if(element.slideElementLink.tooltipText == undefined)
						 	tooltip = 'title="Click to go to the next slide"';
						else
							tooltip = 'title="'+element.slideElementLink.tooltipText+'"';
						
					}
			}	
		    if(element.slideElementLink.linkTarget == "PREVIOUS"){
				
					if(typeof PRINT_MODE == 'undefined'){
								href = 'Javascript:prev()';
					}
					else
					{
						
					}
					if(element.slideElementLink.tooltipEnabled){
						if(element.slideElementLink.tooltipText == undefined)
						 	tooltip = 'title="Click to go to the previous slide"';
						else
							tooltip = 'title="'+element.slideElementLink.tooltipText+'"';
						
					}
			} 
			if(element.slideElementLink.linkTarget == "FIRST") {
					if(typeof PRINT_MODE == 'undefined'){
							href = 'Javascript:goToSlide(0)';
					}
					else
					{
							href = '#page=1';
					}
					if(element.slideElementLink.tooltipEnabled){
						if(element.slideElementLink.tooltipText == undefined)
						 	tooltip = 'title="Click to go to the first slide"';
						else
							tooltip = 'title="'+element.slideElementLink.tooltipText+'"';
						
					}
			}
			
		}
		else if(element.slideElementLink.linkType == "URL"){
			href = element.slideElementLink.linkTarget;
			target = 'target="_blank"';
			if(element.slideElementLink.tooltipEnabled){
				if(element.slideElementLink.tooltipText == undefined)
				 	tooltip = 'title="Click to go to \''+element.slideElementLink.linkTarget+'\'"';
				else
					tooltip = 'title="'+element.slideElementLink.tooltipText+'"';
				
			}

		}
		
		return '<a href="'+href+'"  '+target+' '+tooltip+'>';
}



SlideBuilder.prototype.renderVideoElement = function(element){


	

	
	var shadowStyle = '';
	var clipStyle = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;
	var dx = 0;
	var dy = 0;
	var w = Number(element.resourceContent.width)-Number(element.renderingInfo.cropRight) -Number(element.renderingInfo.cropLeft);
	var w1 = Number(element.resourceContent.width)-Number(element.renderingInfo.cropRight)*1.32 -Number(element.renderingInfo.cropLeft);
	var h = Number(element.resourceContent.height)-Number(element.renderingInfo.cropBottom)-Number(element.renderingInfo.cropTop);
	
	/*
		element.startOnClick;
		element.playButtonDisplay;
		element.playbackControlsDisplay
	*/
	var controls = " autoplay  data-autoplay ";
	var endevents = "";
	var playpauseLayer = "";
	var PLAY_BUTTON_WIDTH = 256;
	var posterFrame = MEDIA_CDN_PATH+'posterframe/'+element.resourceContent.firstFrame;
	
	//if video is part of build do not autplay
	if(this.currentSlide.actions[element.id])
	       controls = "";
	
	if(element.startOnClick == "true")
			controls = 'class="zvideo"';
			
	if(element.loop == "true")
			controls += ' loop ';
	
	if(element.muteAudio == "true")
			controls += ' muted ';
	
	if(element.advanceOnComplete == "true")
			endevents = 'next();';
	
	if(element.startOnClick == "true"){
		posterFrame = MEDIA_CDN_PATH+'posterframe/'+element.resourceContent.posterFrame;
	}
	

	
	shadowStyle = this.getDropShadowStyle(element);
	clipStyle = this.getCropStyle(element);
	
	//*Math.abs(1-window.devicePixelRatio)
		var rx = Number(element.background.shape.cornerRadius)*(1/Number(element.frame.scaleX))
	
	if(Number(element.background.stroke.weight) > 0 ){
		strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
	
	
	 	strokeRect = 	'<rect id="border-'+element.id+'" x="'+Number(element.renderingInfo.cropLeft)+'" y="'+Number(element.renderingInfo.cropTop)+'"  width="'+w+'" height="'+h+'" rx="'+rx+'" ry="'+rx+'" style="'+strokeWidth+'fill-opacity:0;"  />';
					
	}
	
	videoXml = "";
		if(element.startOnClick == "true" && element.playButtonDisplay == "true" && (typeof PRINT_MODE == 'undefined')) {
			var top = (1/Number(element.frame.scaleX))*Number(element.frame.height)/2-128;
			var left =  (1/Number(element.frame.scaleX))*Number(element.frame.width)/2-128;
			var right = 0;
			var bottom = 0;
			if(element.renderingInfo.cropEnabled == "true" || Number(element.background.shape.cornerRadius) > 0){

					top += Number(element.renderingInfo.cropTop);
					left += Number(element.renderingInfo.cropLeft);
					right = Number(element.renderingInfo.cropRight);
					bottom = Number(element.renderingInfo.cropBottom);
			}
			endevents += 'videoEnded('+element.id+');';
			videoXml += '<div class="playpause" style="z-index:2;top:'+top+'px;left:'+left+'px;right:'+right+'px;bottom:'+bottom+'px;-webkit-transform: scale('+(1/Number(element.frame.scaleX))+', '+(1/Number(element.frame.scaleX))+');-moz-transform: scale('+(1/Number(element.frame.scaleX))+', '+(1/Number(element.frame.scaleX))+');transform: scale('+(1/Number(element.frame.scaleX))+', '+(1/Number(element.frame.scaleX))+');cursor:hand"></div>';
		}

	videoXml +='<svg width="'+(w+dx)+'" height="'+(h+dy)+'"  style="z-index:1;position:absolute;border: 0px solid black;"  version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" >'+
			

					strokeRect +
	'</svg>';
	
	var videoEle = '';
	
		if(typeof PRINT_MODE == 'undefined'){
			videoEle =	'<video  id="video-'+element.id+'" '+controls+' onended="'+endevents+'"  playsinline data-paused-by-reveal poster="'+posterFrame+'" data-src="'+MEDIA_CDN_PATH+'video/'+element.resourceContent.filename+'"  style="'+clipStyle+'width:'+(element.resourceContent.width)+'px;height:'+(element.resourceContent.height)+'px;">'+
				'<source src="" type="video/mp4" preload="metadata"></source>'+
				'</video>'+playpauseLayer;
		}
		else{
			videoEle =	'<img src="'+MEDIA_CDN_PATH+'posterframe/'+element.resourceContent.posterFrame+'" style="'+clipStyle+'width:'+(element.resourceContent.width)+'px;height:'+(element.resourceContent.height)+'px;"></img>';

		}
	
	


	videoXml +=	'<div style="'+shadowStyle+'" >'+
				videoEle+
		'</div>';
		
	return videoXml;

}
SlideBuilder.prototype.getDropShadowStyle = function(element){
		if(element.filter.dropShadow.enabled == "true"){
			var dx = 0;
			var dy = 0;
			var blur = 0;
			var color = "";
				//we get offset angle, and we need to map to x,y coordinate
				//https://forums.tigsource.com/index.php?topic=34039.0
				//x= cos(angle)
				//y= sin(angle)
				//we substract element rotation from shaow angle, as shadow effect should be applied on transformend object
				//we add stroke weight 
			 dx = Math.cos(Number(element.filter.dropShadow.angle)-Number(element.frame.rotationAngle))*Number(element.filter.dropShadow.distance)*1/Number(element.frame.scaleX)+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX);
			  dy= Math.sin(Number(element.filter.dropShadow.angle)-Number(element.frame.rotationAngle))*Number(element.filter.dropShadow.distance)*1/Number(element.frame.scaleX) + Number(element.background.stroke.weight)*1/Number(element.frame.scaleX);
			blur = Number(element.filter.dropShadow.blur)*1/3;
			color = element.filter.dropShadow.color;
			var shadowParams = "drop-shadow("+dx+"px "+dy+"px "+blur+"px "+color+")";
			return "filter:"+ shadowParams +";-webkit-filter:"+ shadowParams +";";
		}
		return "";
	
}

SlideBuilder.prototype.getCropStyle = function(element){
	if(element.renderingInfo.cropEnabled == "true" || Number(element.background.shape.cornerRadius) > 0){
		
			var clipParams = "inset("+element.renderingInfo.cropTop+"px "+element.renderingInfo.cropRight+"px "+element.renderingInfo.cropBottom+"px "+element.renderingInfo.cropLeft+"px round "+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+"px "+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+"px)";
			return "clip-path:"+ clipParams +";-webkit-clip-path:"+ clipParams +";";
		}
		return "";
	
}
SlideBuilder.prototype.renderImageElement_old = function(element){
	//detectIE() != false
	if(detectIE() != false) return this.renderImageElementforIE(element);
	
	var shadowStyle = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;///Number(element.frame.scaleX);
	var dx = 0;
	var dy = 0;
	var w = Number(element.resourceContent.width);
	var h = Number(element.resourceContent.height);
	
	
	//it is observed old files do not have cropping support
	if(element.renderingInfo["cropRight"]){
		w =w -Number(element.renderingInfo.cropRight) -Number(element.renderingInfo.cropLeft);
		h = h -Number(element.renderingInfo.cropBottom)-Number(element.renderingInfo.cropTop);
	}

	if(Number(element.background.fill.alpha) > 0){
		filterStyle = 'filter="url(#fill-'+element.id+')"';
		filterDef	= '<filter id="fill-'+element.id+'" y="-40%" height="180%">'+
							   '<feColorMatrix  type="matrix" values="'+element.background.fill.colorHex+'" />'+
						 		
						'</filter>';
	}
	shadowStyle = this.getDropShadowStyle(element);
	var clipStyle = "";

	
	if(Number(element.background.stroke.weight) > 0 ){
		var cropX = 0;
		var cropY = 0;
		if(element.renderingInfo["cropLeft"]) cropX = Number(element.renderingInfo.cropLeft);
		if(element.renderingInfo["cropTop"]) cropY = Number(element.renderingInfo.cropTop);
		
		strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
		strokeRect = 	'<rect id="border-'+element.id+'" x="'+cropX+'" y="'+cropY+'"  width="'+w+'" height="'+h+'" rx="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" ry="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" style="'+strokeWidth+'fill-opacity:0;"  />';
					
	}
	
	clipStyle = this.getCropStyle(element);

	
	imageXml ='<svg width="'+(w+dx)+'" height="'+(h+dy)+'"  style="z-index:1;position:absolute;border: 0px solid black;"  version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" >'+
			

						strokeRect +
			
	'</svg>';
	imageXml +=	'<div style="'+shadowStyle+';background-color:'+element.background.fill.color+';width:'+w+'px;height:'+h+'px;" >'+
		'<img src="'+MEDIA_CDN_PATH+'image/'+element.resourceContent.filename+'" style="'+clipStyle+';/*mix-blend-mode:multiply*/"></img>'+
		'</div>';
	
	return imageXml;
}

SlideBuilder.prototype.renderImageElement = function(element){
		//tanslateZ(0) is done to remove blurness due to scale and rotate transformation performed



		var shadowStyle = '';
		var strokeRect = "";
		var normalizeFactor = 1/3;///Number(element.frame.scaleX);
		var dx = 0;
		var dy = 0;
		var w = Number(element.resourceContent.width);
		var h = Number(element.resourceContent.height);

		//it is observed old files do not have cropping support
		if(element.renderingInfo["cropRight"]){
			w =w -Number(element.renderingInfo.cropRight) -Number(element.renderingInfo.cropLeft);
			h = h -Number(element.renderingInfo.cropBottom)-Number(element.renderingInfo.cropTop);
		}
		var filterStyle = "";
		var filterStyle2 = "";
		var filterDef = "";
		var opacity = "";

		if(Number(element.background.fill.alpha) > 0){
			filterStyle = 'filter="url(#fill-'+element.id+')"';
			filterStyle2 = 'filter:url(#fill-'+element.id+');-webkit-filter:url(#fill-'+element.id+');';
			opacity = "opacity:1;"
			filterDef	= '<filter id="fill-'+element.id+'">'+
								   '<feFlood filterUnits="userSpaceOnUse" result="flood-'+element.id+'"  flood-color="'+element.background.fill.color2+'" flood-opacity="'+element.background.fill.alpha+'"/>'+
									' <feBlend in="SourceGraphic" in2="flood-'+element.id+'" mode="normal" />'+
							'</filter>';
		}
		shadowStyle = this.getDropShadowStyle(element);

		if(Number(element.background.stroke.weight) > 0 ){
			var cropX = 0;
			var cropY = 0;
			if(element.renderingInfo["cropLeft"]) cropX = Number(element.renderingInfo.cropLeft);
			if(element.renderingInfo["cropTop"]) cropY = Number(element.renderingInfo.cropTop);

			strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
			strokeRect = 	'<rect id="border-'+element.id+'" x="'+cropX+'" y="'+cropY+'"  width="'+w+'" height="'+h+'" rx="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" ry="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" style="'+strokeWidth+'fill-opacity:0;"  />';

		}
		var clipStyle = "";
		var clipStyle2 = "";
		var clipDef = "";
		if(element.renderingInfo["cropLeft"] ){
		 	clipStyle = 'clip-path= url(#crop-'+element.id+')';	
			clipStyle2 = 'clip-path:url(#crop-'+element.id+');';
		 	clipDef =	' <clipPath id="crop-'+element.id+'" height="100%">'+
			         '   <rect x="'+Number(element.renderingInfo.cropLeft)+'" y="'+Number(element.renderingInfo.cropTop)+'"  width="'+w+'" height="'+h+'" rx="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" ry="'+Number(element.background.shape.cornerRadius)*1/Number(element.frame.scaleX)+'" />'+
				    '</clipPath>';

				/*
				clipDef =	' <clipPath id="crop-'+element.id+'" height1="100%" clipPathUnits="objectBoundingBox">'+
				         '   <rect x="'+Number(element.renderingInfo.cropLeft)/Number(element.resourceContent.width)+'" y="'+Number(element.renderingInfo.cropTop)/Number(element.resourceContent.height)+'"  width="'+w/Number(element.resourceContent.width)+'" height="'+h/Number(element.resourceContent.height)+'" rx="'+(1/Number(element.background.shape.cornerRadius))+'" ry="'+(1/Number(element.background.shape.cornerRadius))+'" />'+
					    '</clipPath>';

				*/
		}
		var imageTag = "";
		//filter="url(#myFilter)"
		if(detectIE() == false){
			imageTag = 	'<foreignObject  width="100%" height="100%" '+clipStyle+' style="'+filterStyle2+'">'+
					'<img src="'+MEDIA_CDN_PATH+'image/'+element.resourceContent.filename+'" style=""></img>'+
	        	'</foreignObject>';
		}
		else{
			imageTag = '<image x="0" y="0" '+filterStyle+' '+clipStyle+' xlink:href="'+MEDIA_CDN_PATH+'image/'+element.resourceContent.filename+'" height="100%" width="100%"  style=""/>';
		}

		imageXml ='<svg version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="'+(Number(element.resourceContent.width))+'" height="'+(Number(element.resourceContent.height))+'" viewbox="0 0 '+Number(element.resourceContent.width)+' '+Number(element.resourceContent.height)+'"  style="border: 0px solid black; '+shadowStyle+'">'+
					'<defs>'+
				/*		'<filter id="myFilter">'+
							'<feColorMatrix  type="matrix" values="'+element.background.fill.colorHex+'" result="flood"/>'+
					          ' <feBlend in2="SourceGraphic" in="flood" mode="multiply" />'+
					     '   </filter>'+
					*/
						filterDef+
						clipDef+
					'</defs>'+
					imageTag +
					strokeRect +

		'</svg>';


		return imageXml;

	}





SlideBuilder.prototype.parseChartData = function(element){
	 var chart = {};
	var chartTitle = element.name;
	
	var chartProperties = element.chart.options.chartProperties;
var options = element.chart.options;
    var type = chartProperties.chartLayoutOptions.type['$t'];
      var chartLayoutOptions = chartProperties.chartLayoutOptions;
      var chartOptions = chartProperties.chartOptions;
      var chartTextFormatOptions = chartProperties.chartTextFormatOptions;
      var chartXAxisOptions = chartProperties.chartXAxisOptions;
      var chartYaxisOptions = chartProperties.chartYaxisOptions;


	switch(type) {
	        case 'column':
	          
	          if(options.chartProperties.chartOptions.showShadow['$t']=="true")
	          {
	            chart.type = "ShadowBar";
	          }
	          else
	          {
	            chart.type = "bar";
	          }
	          chart.data = {};
	          chart.data.datasets = [];
	          chart.data.labels = []; 
	          chart.options = {};

	          _.each(element.chart.data.chart.columns, function(column, index){
	            var temp = [];
	            temp[index] = {label:column['label']}
	            chart.data.datasets[index] = {label:column['label']};
	          });

	          var axesScaleValues = [];

	          _.each(element.chart.data.chart.rows.row, function(row, index){
	            chart.data.labels.push(row.category);
	            var keys = Object.keys(row);
	            _.each(keys, function(key){
	              if(chart.data.datasets[key])
	              {
	                chart.data.datasets[key]['backgroundColor'] = toColor(row[key]['color'],1); 
	                if(!chart.data.datasets[key]['data'])
	                {
	                  chart.data.datasets[key]['data'] = []
	                }
	                chart.data.datasets[key]['data'].push(row[key]['$t']);
	                axesScaleValues.push(row[key]['$t']);
	              }
	            })
	          })

	          var maxValue = axesScaleValues.reduce(function(a, b) { return Math.max(a, b); }); 

	          for( key in chart['data']['datasets'] )
	          {
	            chart['data']['datasets'].push(chart['data']['datasets'][key]);
	            delete chart['data']['datasets'][key];
	          }

	          var xAxes = {
	            ticks: {
	              beginAtZero: true,
	              minor:{
	                display:true
	              }
	            },
	            scaleLabel: {
	              display: (chartXAxisOptions.showXaxisTitle['$t'] == "true"),
	              labelString: chartXAxisOptions.xAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartYaxisOptions.showYaxisGridLines['$t']==="true",
	              color: toColor(chartXAxisOptions.xAxisColor['$t'],1)
	            },
	            barPercentage: Number(chartOptions.barWidth['$t'])/100,
	            categoryPercentage: 100/100,
	            stacked: chartOptions.multipleSeries['$t']=='Stacked'
	          }

	          var yAxes = {
	            ticks:{
	              beginAtZero: true,
	              minor:{
	                display:true
	              },
	              max:  (maxValue  + Math.ceil(maxValue/10))
	            },
	            scaleLabel: {
	              display: (chartYaxisOptions.showYaxisTitle['$t']== "true"),
	              labelString: chartYaxisOptions.yAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartXAxisOptions.showXaxisGridLines['$t']==="true",
	              color: toColor(chartYaxisOptions.yAxisColor['$t'],1)
	            },
	            barPercentage: Number(chartOptions.barWidth['$t'])/100,
	            stacked:chartOptions.multipleSeries['$t']=='Stacked'
	          }
	          var legend = {
	            position: chartProperties.chartLayoutOptions.legendLocation['$t'].toLowerCase()
	          }
	          var title = {
	            display: chartProperties.chartLayoutOptions.showChartTitle['$t']==="true",
	            text:chartTitle,
	            fontFamily: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFont['$t'],
	            fontColor: toColor(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontColor['$t'],1),
	            fontSize: parseInt(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t']),
	            fontStyle: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t']
	          }
	          labelPosition = chartOptions.dataLabelPositionColumn['$t'];

	          var plugins = {
	            datalabels:{
	              display:labelPosition!=="None",
	              align:labelPosition=='Outside'?'top':labelPosition=='Top'?'bottom':'end',
	              anchor:labelPosition=='Outside' || labelPosition=='Top'?'end':'start'
	            }
	          }


	          chart.options = {
	            responsive:true,
	            scales:{
	              xAxes:[xAxes],
	              yAxes:[yAxes]
	            },
	            legend:legend,
	            title: title,
	            plugins:plugins
	          }

	          
	        break;
	        case 'line':
	          
	          if(options.chartProperties.chartOptions.showShadow['$t']=="true")
	          {
	            chart.type = "ShadowLine";
	          }
	          else
	          {
	            chart.type = "line";
	          }
	          chart.data = {};
	          chart.data.datasets = [];
	          chart.data.labels = []; 
	          chart.options = {};

	          _.each(element.chart.data.chart.columns, function(column, index){
	            var temp = [];
	            temp[index] = {label:column['label']}
	            chart.data.datasets[index] = {label:column['label']};
	          });

	          var axesScaleValues = [];

	          _.each(element.chart.data.chart.rows.row, function(row, index){
	            chart.data.labels.push(row.category);
	            var keys = Object.keys(row);
	            _.each(keys, function(key){
	              if(chart.data.datasets[key])
	              {
	                chart.data.datasets[key]['borderColor'] = toColor(row[key]['color'],1); 
	                chart.data.datasets[key]['pointBackgroundColor'] = toColor(row[key]['color'],1);
	                chart.data.datasets[key]['backgroundColor'] = "transparent"; 
	                chart.data.datasets[key]['borderWidth'] = chartOptions.lineWeight['$t']; 
	                chart.data.datasets[key]['pointRadius'] = chartOptions.dataPointsSize['$t']%10; 
	                if(!chart.data.datasets[key]['data'])
	                {
	                  chart.data.datasets[key]['data'] = []
	                }
	                chart.data.datasets[key]['data'].push(row[key]['$t']);
	                axesScaleValues.push(row[key]['$t']);

	              }
	            })
	          })
	          var maxValue = axesScaleValues.reduce(function(a, b) { return Math.max(a, b); }); 

	          for( key in chart['data']['datasets'] )
	          {
	            chart['data']['datasets'].push(chart['data']['datasets'][key]);
	            delete chart['data']['datasets'][key];
	          }
	          var xAxes = {
	            ticks:{
	              beginAtZero: true
	            },
	            scaleLabel: {
	              display: (chartXAxisOptions.showXaxisTitle['$t'] == "true"),
	              labelString: chartXAxisOptions.xAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartXAxisOptions.showXaxisGridLines['$t']==="true",
	              color: toColor(chartXAxisOptions.xAxisColor['$t'],1)
	            },
	            barPercentage: Number(chartOptions.barWidth['$t'])/100
	          }
	          var yAxes = {
	            ticks:{
	              beginAtZero: true,
	              padding:50,
	              max:  (maxValue  + Math.ceil(maxValue/10))
	            },
	            scaleLabel: {
	              display: (chartYaxisOptions.showYaxisTitle['$t']== "true"),
	              labelString: chartYaxisOptions.yAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartYaxisOptions.showYaxisGridLines['$t']==="true",
	              color: toColor(chartYaxisOptions.yAxisColor['$t'],1)
	            },
	            barPercentage: Number(chartOptions.barWidth['$t'])/100
	          }
	          var legend = {
	            position: chartProperties.chartLayoutOptions.legendLocation['$t'].toLowerCase()
	          }
	          var title = 
	          {
	            display: chartProperties.chartLayoutOptions.showChartTitle['$t']==="true",
	            text:chartTitle,
	            fontFamily: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFont['$t'],
	            fontColor: toColor(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontColor['$t'],1),
	            fontSize: parseInt(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t'])
	          }

	          var plugins = {
	            datalabels:{
	              display:false
	            }
	          }

	          var elements = {
	            line:{
	              tension:chartOptions.lineType['$t']=='Curved'?0.4:0
	            }
	          };

	          chart.options = {
	            responsive:false,
	            scales:{
	              xAxes:[xAxes],
	              yAxes:[yAxes]
	            },
	            legend:legend,
	            title: title,
	            plugins: plugins,
	            elements: elements,
	            layout:{
	              padding:{
	                top: 10
	              }
	            }
	          }
	          
	        break;

	        case "area":
	          
	          chart.type = "line";
	          chart.data = {};
	          chart.data.datasets = [];
	          chart.data.labels = []; 
	          chart.options = {};

	          _.each(element.chart.data.chart.columns, function(column, index){
	            var temp = [];
	            temp[index] = {label:column['label']}
	            chart.data.datasets[index] = {label:column['label']};
	          });
	          var axesScaleValues = [];

	          _.each(element.chart.data.chart.rows.row, function(row, index) {
	            chart.data.labels.push(row.category);
	            var keys = Object.keys(row);
	            _.each(keys, function(key){
	              if(chart.data.datasets[key])
	              {
	                chart.data.datasets[key]['borderColor'] = toColor(row[key]['color'],1); 
	                chart.data.datasets[key]['backgroundColor'] = toColor(row[key]['color'],1); 
	                chart.data.datasets[key]['borderWidth'] = chartOptions.lineWeight['$t']; 
	                chart.data.datasets[key]['pointRadius'] = 0; 
	                if(!chart.data.datasets[key]['data'])
	                {
	                  chart.data.datasets[key]['data'] = []
	                }
	                chart.data.datasets[key]['data'].push(row[key]['$t']);
	                axesScaleValues.push(row[key]['$t']);
	              }
	            })
	          })

	          var maxValue = axesScaleValues.reduce(function(a, b) { return Math.max(a, b); }); 

	          for( key in chart['data']['datasets'] )
	          {
	            chart['data']['datasets'].push(chart['data']['datasets'][key]);
	            delete chart['data']['datasets'][key];
	          }
	          chart['data']['datasets'] = chart['data']['datasets'].reverse();
	          var xAxes = {
	            ticks:{
	              beginAtZero: true
	            },
	            scaleLabel: {
	              display: (chartXAxisOptions.showXaxisTitle['$t'] == "true"),
	              labelString: chartXAxisOptions.xAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartXAxisOptions.showXaxisGridLines['$t']==="true",
	              color: toColor(chartXAxisOptions.xAxisColor['$t'],1)
	            }
	          }

	          var yAxes = {
	            ticks: {
	              beginAtZero: true,
	              max:  (maxValue  + Math.ceil(maxValue/10))
	            },
	            scaleLabel: {
	              display: (chartYaxisOptions.showYaxisTitle['$t']== "true"),
	              labelString: chartYaxisOptions.yAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartYaxisOptions.showYaxisGridLines['$t']==="true",
	              color: toColor(chartYaxisOptions.yAxisColor['$t'],1)
	            }
	            // stacked:true
	          }

	          var legend = {
	            position: chartProperties.chartLayoutOptions.legendLocation['$t'].toLowerCase()
	          }
	          var title = {
	            display: chartProperties.chartLayoutOptions.showChartTitle['$t']==="true",
	            text:chartTitle,
	            fontFamily: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFont['$t'],
	            fontColor: toColor(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontColor['$t'],1),
	            fontSize: parseInt(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t'])
	          }

	          var plugins = {
	            datalabels:{
	              display:false
	            }
	          }

	          var elements = {
	            line:{
	              tension:chartOptions.lineType['$t']=='Curved'?0.4:0
	            }
	          };

	          chart.options = {
	            responsive:false,
	            scales:{
	              xAxes:[xAxes],
	              yAxes:[yAxes]
	            },
	            legend:legend,
	            title: title,
	            plugins: plugins,
	            elements:elements
	          }
	          
	        break;
	        case "bar":
	          
	          if(options.chartProperties.chartOptions.showShadow['$t']=="true")
	          {
	            chart.type = "ShadowHorizontalBar";
	          }
	          else
	          {
	            chart.type = "horizontalBar";
	          }
	          chart.data = {};
	          chart.data.datasets = [];
	          chart.data.labels = []; 
	          chart.options = {};

	          _.each(element.chart.data.chart.columns, function(column, index){
	            var temp = [];
	            temp[index] = {label:column['label']}
	            chart.data.datasets[index] = {label:column['label']};
	          });

	          var axesScaleValues = [];

	          _.each(element.chart.data.chart.rows.row, function(row, index){
	            chart.data.labels.push(row.category);
	            var keys = Object.keys(row);
	            _.each(keys, function(key){
	              if(chart.data.datasets[key])
	              {
	                chart.data.datasets[key]['backgroundColor'] = toColor(row[key]['color'],1); 
	                if(!chart.data.datasets[key]['data'])
	                {
	                  chart.data.datasets[key]['data'] = []
	                }
	                chart.data.datasets[key]['data'].push(row[key]['$t']);
	                axesScaleValues.push(row[key]['$t']);
	              }
	            })
	          })

	          var maxValue = axesScaleValues.reduce(function(a, b) { return Math.max(a, b); });
			
	          for( key in chart['data']['datasets'] )
	          {
	            chart['data']['datasets'][key]['data'] =  chart['data']['datasets'][key]['data'].reverse();
	            chart['data']['datasets'].push(chart['data']['datasets'][key]);
	            delete chart['data']['datasets'][key];
	          }
	          var xAxes = {
	            ticks:{
	              beginAtZero: true,
	              max:  (maxValue  + Math.ceil(maxValue/10))
	            },
	            scaleLabel: {
	              display: (chartXAxisOptions.showXaxisTitle['$t'] == "true"),
	              labelString: chartXAxisOptions.xAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartXAxisOptions.showXaxisGridLines['$t']==="true",
	              color: toColor(chartXAxisOptions.xAxisColor['$t'],1)
	            }
	          }
	          var yAxes = {
	            ticks:{
	              beginAtZero: true
	            },
	            scaleLabel: {
	              display: (chartYaxisOptions.showYaxisTitle['$t']== "true"),
	              labelString: chartYaxisOptions.yAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartYaxisOptions.showYaxisGridLines['$t']==="true",
	              color: toColor(chartYaxisOptions.yAxisColor['$t'],1)
	            },
	            categoryPercentage: 100/100,
	            barPercentage: Number(chartOptions.barWidth['$t'])/100
	          }
	          var legend = {
	            position: chartProperties.chartLayoutOptions.legendLocation['$t'].toLowerCase()
	          }

	          labelPosition = chartOptions.dataLabelPositionBar['$t'];

	          var plugins = {
	            datalabels:{
	              display:labelPosition!=="None",
	              align:labelPosition=='Outside'?'end':labelPosition=='Top'?'bottom':'end',
	              anchor:labelPosition=='Outside' || labelPosition=='Top'?'end':'start'
	            }
	          }
	          var title = {
	            display: chartProperties.chartLayoutOptions.showChartTitle['$t']==="true",
	            text:chartTitle,
	            fontFamily: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFont['$t'],
	            fontColor: toColor(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontColor['$t'],1),
	            fontSize: parseInt(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t'])

	          }

	          chart.options = {
	            responsive:false,
	            scales:{
	              xAxes:[xAxes],
	              yAxes:[yAxes]
	            },
	            legend:legend,
	            title: title,
	            plugins:plugins
	          }

	          chart.data.labels = chart.data.labels.reverse();
	          
	        break;

	        case "pie":
	          
	          if(options.chartProperties.chartOptions.showShadow['$t']=="true")
	          {
	            chart.type = "ShadowPie";
	          }
	          else
	          {
	            chart.type = "pie";
	          }
	          chart.data = {};
	          chart.data.datasets = [];
	          chart.data.labels = []; 
	          chart.options = {};
	          var pieData = [];
	          var backgroundColor = [];

	          _.each(element.chart.data.chart.rows.row, function(row, index) {
	            chart.data.labels.push(row.category);

	            for(key in row)
	            {
	              if(Object.keys(row[key]).length == 3)
	              {
	                pieData.push(row[key]['$t']);
	                backgroundColor.push(toColor(row[key]['color'], 1));
	              }
	            }
	          });
	          chart.data.datasets.push({data:pieData,backgroundColor:backgroundColor})

	          var xAxes = {
	            ticks: {
	              beginAtZero: true,
	              minor:{
	                display:true
	              }
	            },
	            scaleLabel: {
	              display: (chartXAxisOptions.showXaxisTitle['$t'] == "true"),
	              labelString: chartXAxisOptions.xAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartXaxisTitleTextFormatOptions.xAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartXAxisOptions.showXaxisGridLines['$t']==="true",
	              color: toColor(chartXAxisOptions.xAxisColor['$t'],1)
	            }
	          }
	          var yAxes = {
	            ticks: {
	              beginAtZero: true,
	              minor:{
	                display:true
	              }
	            },
	            scaleLabel: {
	              display: (chartYaxisOptions.showYaxisTitle['$t']== "true"),
	              labelString: chartYaxisOptions.yAxisTitle['$t'],
	              fontColor: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontColor['$t'],
	              fontSize: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontSize['$t'],
	              fontStyle: chartTextFormatOptions.chartYaxisTitleTextFormatOptions.yAxisTitleFontStyle['$t']
	            },
	            gridLines: {
	              drawOnChartArea: chartYaxisOptions.showYaxisGridLines['$t']==="true",
	              color: toColor(chartYaxisOptions.yAxisColor['$t'],1)
	            }
	          }

	          var legend = {
	            display:chartProperties.chartLayoutOptions.legendLocation['$t']!=='None',
	            position: chartProperties.chartLayoutOptions.legendLocation['$t'].toLowerCase()
	          }
	          var title = {
	            display: chartProperties.chartLayoutOptions.showChartTitle['$t']==="true",
	            text:chartTitle,
	            fontFamily: chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFont['$t'],
	            fontColor: toColor(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontColor['$t'],1),
	            fontSize: parseInt(chartTextFormatOptions.chartTitleTextFormatOptions.chartTitleFontSize['$t'])

	          }

	          labelPosition = chartOptions.pieDataLabelPosition['$t'];

	          var plugins = {
	            datalabels:{
	              display:labelPosition!=='None',
	              anchor:labelPosition=='Outside'?'end':'center',
	              align:labelPosition=='Outside'?'end':'center'
	            }
	          }

	          chart.options = 
	          {
	            responsive:false,
	            title:title,
	            plugins:plugins,
	            legend:legend,
	            layout:{
	              padding:{
	                left: 15,
	                right: 15,
	                top: 15,
	                bottom: 15
	              }
	            }
	          }

	          
	        break;
	      }
	if(chartOptions.showDataRollover['$t'] == "true"){
		chart.options.tooltips = {
		   	position: "nearest",
			mode: 'index',
			intersect: false
		  };
		chart.options.responsive = true;
		chart.options.showTooltips = true;
		

	}
	
	if(isNumeric(chartYaxisOptions.yAxisSteps['$t'])){
			chart.options.scales.yAxes[0].ticks["stepSize"] = chartYaxisOptions.yAxisSteps['$t'];
	}
	if(isNumeric(chartYaxisOptions.yAxisMaxValue['$t'])){
			chart.options.scales.yAxes[0].ticks["suggestedMax"] = chartYaxisOptions.yAxisMaxValue['$t'];
	}
	if(isNumeric(chartYaxisOptions.yAxisMinValue['$t'])){
			chart.options.scales.yAxes[0].ticks["beginAtZero"] =false;
			chart.options.scales.yAxes[0].ticks["suggestedMin"] = chartYaxisOptions.yAxisMinValue['$t'];
	}
	chart.options.animation= false;
	return chart;
}
SlideBuilder.prototype.renderChartElement = function(element){
	
	
	var parsedChart = this.parseChartData(element);


	
	var shadowStyle = '';
	var shadowDef = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;
	var dx = 0;
	var dy = 0;
	var w = Number(element.frame.width)-Number(element.renderingInfo.cropRight) -Number(element.renderingInfo.cropLeft);
	var h = Number(element.frame.height)-Number(element.renderingInfo.cropBottom)-Number(element.renderingInfo.cropTop);

	
	shadowStyle = this.getDropShadowStyle(element);

	
		var rx = Number(element.background.shape.cornerRadius)*(1/Number(element.frame.scaleX))
	
	if(Number(element.background.stroke.weight) > 0 ){
		strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
	
	
	 	strokeRect = 	'<rect id="border-'+element.id+'" x="'+Number(element.renderingInfo.cropLeft)+'" y="'+Number(element.renderingInfo.cropTop)+'"  width="'+w+'" height="'+h+'" rx="'+rx+'" ry="'+rx+'" style="'+strokeWidth+'fill-opacity:0;"  />';
					
	}
	var clipStyle = this.getCropStyle(element);
	

	chartXML = '<div style="width:'+w+';height:'+h+';">';

	chartXML +='<svg width="'+(w+dx)+'" height="'+(h+dy)+'"  style="z-index:1;position:absolute;border: 0px solid black;'+shadowStyle+'" version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" >'+
				
			
					strokeRect +
	'</svg>';
	
var inlineChartData = '';
var seralizeChartData = JSON.stringify(parsedChart);
/*if(typeof PRINT_MODE == 'undefined'){
	
}
else
{
	
	inlineChartData += '<!-- '+
			seralizeChartData+
			'	-->';
	seralizeChartData = parsedChart.type;
}
*/
			chartXML +=	'<canvas id="canvas'+element.id+'" class="stretch" data-chart=\''+seralizeChartData+'\' width="'+element.frame.width+'" height="'+element.frame.height+'" style="'+clipStyle+shadowStyle+';background-color:'+element.background.fill.color+';width:'+(element.frame.width)+'px;height:'+(element.frame.height)+'px;" >'+
			inlineChartData+
            	'</canvas>';
	chartXML +="</div>";
			return chartXML;
	
}


SlideBuilder.prototype.renderPDFViewer = function(element){


	
	var shadowStyle = '';
	var clipStyle = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;
	var dx = 0;
	var dy = 0;
	var w = Number(element.frame.width);
	var h = Number(element.frame.height);
	
	shadowStyle = this.getDropShadowStyle(element);
	
	//*Math.abs(1-window.devicePixelRatio)
		var rx = Number(element.background.shape.cornerRadius)*(1/Number(element.frame.scaleX))
	
	if(Number(element.background.stroke.weight) > 0 ){
		strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
	
	
	 	strokeRect = 	'<rect id="border-'+element.id+'" x="'+Number(element.renderingInfo.cropLeft)+'" y="'+Number(element.renderingInfo.cropTop)+'"  width="'+w+'" height="'+h+'" rx="'+rx+'" ry="'+rx+'" style="'+strokeWidth+'fill-opacity:0;"  />';
					
	}
	
var frame = "";
	if(typeof PRINT_MODE == 'undefined'){
		frame =	'<iframe allowfullscreen="true"  frameborder="0" marginwidth="0" marginheight="0" src="'+element.resourceContent.externalUrl+'" style="width:100%;height:100%;border:'+element.background.stroke.weight+'px solid '+element.background.stroke.color+'" id="html5iframe" name="html5iframe">';
	}
	else{
		frame =	'<img src="'+MEDIA_CDN_PATH+'posterframe/'+element.resourceContent.firstFrame+'" style="width:100%;height:100%"></img>';
		
	}


	pdfXml= '<div style="padding-top:1px;padding-left:1px;overflow:hidden;width:'+w+';height:'+h+';background-color:'+element.background.fill.color+'">'+
					frame+
					'</iframe>'+
					'</div>';
	return pdfXml;

}

SlideBuilder.prototype.renderSpecialViewer = function(element){


	
	var shadowStyle = '';
	var clipStyle = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;
	var dx = 0;
	var dy = 0;
	var w = Number(element.frame.width);
	var h = Number(element.frame.height);
	
	shadowStyle = this.getDropShadowStyle(element);
	
	//*Math.abs(1-window.devicePixelRatio)
		var rx = Number(element.background.shape.cornerRadius)*(1/Number(element.frame.scaleX))
	
	if(Number(element.background.stroke.weight) > 0 ){
		strokeWidth = 'stroke-opacity:1;stroke-width:'+Number(element.background.stroke.weight)*1/Number(element.frame.scaleX)+';stroke:'+element.background.stroke.color+';';
	
	
	 	strokeRect = 	'<rect id="border-'+element.id+'" x="'+Number(element.renderingInfo.cropLeft)+'" y="'+Number(element.renderingInfo.cropTop)+'"  width="'+w+'" height="'+h+'" rx="'+rx+'" ry="'+rx+'" style="'+strokeWidth+'fill-opacity:0;"  />';
					
	}
	
var frame = "";
	if(typeof PRINT_MODE == 'undefined'){
		frame =	'<iframe allowfullscreen="true"  frameborder="0" marginwidth="0" marginheight="0" src="'+element.slideElementLink.linkTarget+'" style="width:100%;height:100%;border:'+element.background.stroke.weight+'px solid '+element.background.stroke.color+'" id="html5iframe" name="html5iframe">';
	}
	else{
		frame =	'<img src="'+MEDIA_CDN_PATH+'posterframe/'+element.resourceContent.firstFrame+'" style="width:100%;height:100%"></img>';
		
	}


	pdfXml= '<div style="padding-top:1px;padding-left:1px;overflow:hidden;width:'+w+';height:'+h+';background-color:'+element.background.fill.color+'">'+
					frame+
					'</iframe>'+
					'</div>';
	return pdfXml;

}


SlideBuilder.prototype.renderTextElement = function(element){


	

	
	var shadowStyle = '';
	var clipStyle = '';
	var strokeRect = "";
	var normalizeFactor = 1/3;
	var dx = 0;
	var dy = 0;
	var w = Number(element.frame.width);
	var h = Number(element.frame.height);
	

	
	shadowStyle = this.getDropShadowStyle(element);
	clipStyle = this.getCropStyle(element);
	
	var rx = Number(element.background.shape.cornerRadius)*(1/Number(element.frame.scaleX))
	
	var padding = 0;
	if(Number(element.background.shape.cornerRadius) > 0)
		padding =  Number(element.background.shape.cornerRadius)/4;
		
	var displayWidth = (element.frame.width-padding*2);
	var displayHeight = (element.frame.height-padding*2);
	
	var zindex = '';
		var strokeWidth = '';
		if(Number(element.background.stroke.weight) > 0 ){
			zindex = 'z-index:1;';
			strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';
	}

	var textXml ='<svg width="'+(w+dx)+'" height="'+(h+dy)+'"  style="'+zindex+'position:absolute;border: 0px solid black;'+shadowStyle+'"  version="1.1"  xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" >'+
			

				'<rect height="'+element.frame.height+'"  width="'+element.frame.width+'" rx="'+element.background.shape.cornerRadius+'" ry="'+element.background.shape.cornerRadius+'" style="'+strokeWidth+'fill-opacity:0;"  />'+
				
	'</svg>';

	textXml +=	'<div class="TextSpan" style="position:fixed;left:0px;padding:'+padding+';'+shadowStyle+';width:'+displayWidth+';height:'+displayHeight+';background-color:'+element.background.fill.color+';'+clipStyle+'" >'+
		element.textInfo.replace(/PwhP/g,displayWidth).replace(/PhtP/g,displayHeight)+
		'</div>';
	return textXml;

}



SlideBuilder.prototype.calculateSquareInEllipse = function(width,height,strokeWeight){
		/**
		 * ctx - context
		 * cx/cy - center of circle
		 * radius - radius of circle
		*/
	/*	function squareInCircle(ctx, cx, cy, radius) {

		    var side = Math.sqrt(radius * radius * 2),  // calc side length of square
		        half = side * 0.5;                      // position offset

		    ctx.strokeRect(cx - half, cy - half, side, side);
		}
		*/
		var square = {};
		var cx = width/2;
		var cy = height/2;	
		var rx = width/2;
		var ry = height/2;


		var sideX = Math.sqrt(rx * rx * 2);		
		var sideY = Math.sqrt(ry * ry * 2);
		var halfX = sideX * 0.5;   
		var halfY = sideY * 0.5;
		var padding = 1+Number(strokeWeight);
		var displayWidth = (sideX-padding*2);
		var displayHeight = (sideY-padding*2);
		
		square.left = 	(cx-halfX+padding);
		square.top = 	(cy-halfY+padding);
		square.width = 	displayWidth;
		square.height = displayHeight;
		
		return square;
}

SlideBuilder.prototype.renderShapeElement = function(element){
	var shapeXml = "";
	var shadowDef = "";
	var shadowStyle = "";
	var dx = 0;
	var dy = 0;
	var w = Number(element.frame.width);
	var h = Number(element.frame.height);
	
		
	

		shadowStyle = this.getDropShadowStyle(element);
	
	if(element.background.shape.type == "Text"){
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';
			
				
			var padding = 1+Number(element.background.stroke.weight)+ Number(element.background.shape.cornerRadius)/4;
			var displayWidth = (element.frame.width-padding*2);
			var displayHeight = (element.frame.height-padding*2);
			shapeXml = '<svg class="sd-element-shape"  height="'+(h+dx*2)+'"  width="'+(w+dy*2)+'"  xmlns="http://www.w3.org/2000/svg" version="1.1"  style="'+shadowStyle+'">'+
							'<defs>'+ shadowDef + '</defs>'+
						'<rect height="'+element.frame.height+'"  width="'+element.frame.width+'" rx="'+element.background.shape.cornerRadius+'" ry="'+element.background.shape.cornerRadius+'" style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;"  />'+
					     '<foreignObject x="'+(padding)+'" y="'+(padding)+'" width="'+displayWidth+'" height="'+displayHeight+'"  style="word-wrap: break-word;overflow: hidden;" >'+
			               	element.textInfo.replace(/PwhP/g,displayWidth).replace(/PhtP/g,displayHeight)+
			            '</foreignObject>'+
						'</svg>';
		
	}
	
	
	

  else if (element.background.shape.type == "Circle")
	 {
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';

				var textArea = this.calculateSquareInEllipse(element.frame.width,element.frame.height,element.background.stroke.weight);
				
					
		shapeXml = '<svg class="sd-element-shape" height="'+(h+dx*2)+'"  width="'+(w+dy*2)+'"  xmlns="http://www.w3.org/2000/svg" version="1.1" preserveAspectRatio="none" style="'+shadowStyle+'">'+
				 	'<circle cx="'+element.frame.width/2+'" cy="'+element.frame.height/2+'" r="'+element.frame.height/2+'" style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;" />'+
				  
				'</svg>'+
				'<div style="position:fixed;left:'+textArea.left+';top:'+textArea.top+';overflow:hidden;width:'+textArea.width+';height:'+textArea.height+';" >'+
			             element.textInfo.replace(/PwhP/g,textArea.width).replace(/PhtP/g,textArea.height)+
		            '</div>';
	}
	else if (element.background.shape.type == "Ellipse")
	 {
			
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';

			     var textArea = this.calculateSquareInEllipse(element.frame.width,element.frame.height,element.background.stroke.weight);
			
				
		shapeXml = '<svg class="sd-element-shape"  height="'+(h+dx*2)+'"  width="'+(w+dy*2)+'"  xmlns="http://www.w3.org/2000/svg" version="1.1"  style="'+shadowStyle+'">'+
						'<defs>'+ shadowDef + '</defs>'+
						'<ellipse cx="'+element.frame.width/2+'" cy="'+element.frame.height/2+'" rx="'+element.frame.width/2+'" ry="'+element.frame.height/2+'" style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;" />	'+
					'</svg>'+
					'<div style="position:fixed;left:'+textArea.left+'px;top:'+textArea.top+'px;overflow:hidden;width:'+textArea.width+'px;height:'+textArea.height+'px;" >'+
				             element.textInfo.replace(/PwhP/g,textArea.width).replace(/PhtP/g,textArea.height)+
			            '</div>';
								
	}
	else if (element.background.shape.type == "Rectangle")
	 {
			
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';
				
			var padding = 1+Number(element.background.stroke.weight);	
			var displayWidth = (element.frame.width-padding*2);
			var displayHeight = (element.frame.height-padding*2);
				
			shapeXml = '<svg class="sd-element-shape" height="'+(h+dx*2)+'"  width="'+(w+dy*2)+'"  xmlns="http://www.w3.org/2000/svg" version="1.1"  style="overflow: visible !important;'+shadowStyle+'">'+
							'<defs>'+ shadowDef + '</defs>'+
							'<rect height="'+element.frame.height+'"  width="'+element.frame.width+'" rx="'+element.background.shape.cornerRadius+'" ry="'+element.background.shape.cornerRadius+'" style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;"  />	'+
						  
						'</svg>'+
						'<div style="position:fixed;left:'+padding+'px;top:'+padding+'px;overflow:hidden;width:'+displayWidth+'px;height:'+displayHeight+'px;" >'+
					             element.textInfo.replace(/PwhP/g,displayWidth).replace(/PhtP/g,displayHeight)+
				            '</div>';
			//Special for Chetan Simon demo
			if(element.name == "pdf-viewer"){
				if(element.slideElementLink.linkEnabled){
				
					shapeXml = this.renderSpecialViewer(element);
				}


			}
	}
	else if (element.background.shape.type == "RoundedRectangle")
	 {
	
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';

				
			var padding = Number(element.background.stroke.weight) +Number(element.background.shape.cornerRadius)/4;
			var displayWidth = (element.frame.width-padding*2);
			var displayHeight = (element.frame.height-padding*2);
			shapeXml = '<svg class="sd-element-shape" height="'+(h+dx*2)+'"  width="'+(w+dy*2)+'"  xmlns="http://www.w3.org/2000/svg" version="1.1"  style="'+shadowStyle+'">'+
							'<defs>'+ shadowDef + '</defs>'+
						'<rect height="'+element.frame.height+'"  width="'+element.frame.width+'" rx="'+element.background.shape.cornerRadius+'" ry="'+element.background.shape.cornerRadius+'" style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;"  />'+
					    
						'</svg>'+
							'<div style="position:fixed;left:'+padding+';top:'+padding+';overflow:hidden;width:'+displayWidth+';height:'+displayHeight+';" >'+
						             element.textInfo.replace(/PwhP/g,displayWidth).replace(/PhtP/g,displayHeight)+
					            '</div>';
		
	}
	else if (element.background.shape.type == "Triangle")
	 {
			var strokeWidth = '';
			if(Number(element.background.stroke.weight) > 0 )
				strokeWidth = 'stroke-opacity:1;stroke-width:'+element.background.stroke.weight+';stroke:'+element.background.stroke.color+';';

			var padding = 0;//1+Number(element.background.stroke.weight);
			var displayWidth = (element.frame.width/2-padding*2);
			var displayHeight = (element.frame.height/2-padding*2);
			var fw = (w+dy*2);
			var fh = (h+dx*2);

			shapeXml = '<svg class="sd-element-shape" height="'+fh+'"  width="'+fw+'"  style="fill:'+element.background.fill.color+';'+strokeWidth+'fill-opacity:1;'+shadowStyle+'"  xmlns="http://www.w3.org/2000/svg" version="1.1"  preserveAspectRatio="none" >'+
						'<defs>'+ shadowDef + '</defs>'+
					  	'<polygon points="'+fw/2+',0 '+fw+','+fh+' 0,'+fh+'"  />'+

					'</svg>'+
					'<div style="position:fixed;padding:'+padding+';left:'+(Number(element.frame.width)/4+padding)+';top:'+(Number(element.frame.height)/2+padding)+';overflow:hidden;width:'+displayWidth+';height:'+displayHeight+';" >'+
				             element.textInfo.replace(/PwhP/g,displayWidth).replace(/PhtP/g,displayHeight)+
			            '</div>';


	}
	else if(element.background.shape.type == "Line")
	{
			
	//	shapeXml = '<svg  height="'+element.background.stroke.weight+'"  width="'+element.frame.width+'" fill="'+element.background.fill.color+'" stroke-width="'+element.background.stroke.weight+'" stroke="'+element.background.stroke.color+'"  xmlns="http://www.w3.org/2000/svg" version="1.1" viewBox="0 0 100 1" preserveAspectRatio="none"><line x1="0" y1="1" x2="100" y2="1"  /></svg>';
		var lineBackground = this.buildLineWithEndType(element);
		var topAdjustment = -1*Number(element.background.stroke.weight)*2;
		
		/*backup of perfectly working markers
			{ id: this.markerIndex++, name: 'circle', path: 'M 0, 0  m -5, 0  a 5,5 0 1,0 10,0  a 5,5 0 1,0 -10,0', viewbox: '-6 -6 12 12', fill: 'orange' }
			    , { id: this.markerIndex++, name: 'square', path: 'M 0,0 m -5,-5 L 5,-5 L 5,5 L -5,5 Z', viewbox: '-5 -5 10 10', fill: 'red' }
			    , { id: this.markerIndex++, name: 'arrow', path: 'M 0,0 m -5,-5 L 5,0 L -5,5 Z', viewbox: '-5 -5 10 10', fill: 'pink' }
			    , { id: this.markerIndex++, name: 'stub', path: 'M 0,0 m -1,-5 L 1,-5 L 1,5 L -1,5 Z', viewbox: '-1 -5 2 10', fill: 'blue' }
				This is a Move To (M), Line To (L), Arc To (A), Line To (L), Arc To (A), Line To (L), Close Path (Z).

				<path d="M100,100 h200 a20,20 0 0 1 20,20 v200 a20,20 0 0 1 -20,20 h-200 a20,20 0 0 1 -20,-20 v-200 a20,20 0 0 1 20,-20 z" />

				Expaination:

				M100,100: Move To Point(100,100)

				h200: Draw A 200px Horizontal Line From Where We Are

				a20,20 0 0 1 20,20: Draw An Arc With 20px X Radius, 20px Y Radius, Clockwise, To A Point With 20px Difference In X And Y Axis

				v200: Draw A 200px Vertical Line From Where We Are

				a20,20 0 0 1 -20,20: Draw An Arc With 20px X And Y Radius, Clockwise, To A Point With -20px Difference In X And 20px Difference In Y Axis

				h-200: Draw A -200px Horizontal Line From Where We Are

				a20,20 0 0 1 -20,-20: Draw An Arc With 20px X And Y Radius, Clockwise, To A Point With -20px Difference In X And -20px Difference In Y Axis

				v-200: Draw A -200px Vertical Line From Where We Are

				a20,20 0 0 1 20,-20: Draw An Arc With 20px X And Y Radius, Clockwise, To A Point With 20px Difference In X And -20px Difference In Y Axis

				z: Close The Path

				<svg width="440" height="440">
				  <path d="M100,100 h200 a20,20 0 0 1 20,20 v200 a20,20 0 0 1 -20,20 h-200 a20,20 0 0 1 -20,-20 v-200 a20,20 0 0 1 20,-20 z" fill="none" stroke="black" stroke-width="3" />
				</svg>
		*/
		
		shapeXml = 	' <svg class="sd-element-shape"  width="'+(Number(element.frame.width)+dx+30)+'" height="'+(Number(element.background.stroke.weight)*4+dx+30)+'" xmlns="http://www.w3.org/2000/svg" style="overflow:hidden;position:absolute;top:'+topAdjustment+'px;'+shadowStyle+'"  >'+
					 ' <defs>'+
					'	<marker id="arrow-solid-dest-'+element.id+'" refX="6.6" refY="0" viewBox="-2 -5 10 12" markerWidth="5" markerHeight="5" orient="auto" markerUnits="strokeWidth" >'+
					'	      <path d="M0-5l10 5l-10 5z" fill="'+element.background.stroke.color+'" />'+
					'	    </marker>'+
					'	<marker id="arrow-solid-origin-'+element.id+'" viewBox="-10 -5 10 12" refX="-4"  orient="auto" markerWidth="5" markerHeight="5" markerUnits="strokeWidth">'+
					  '    <path d="M0-5l-10 5l10 5z" fill="'+element.background.stroke.color+'"/>'+
					  '  </marker>'+
						'	<marker id="circle-origin-'+element.id+'" markerWidth="5" markerHeight="5" refX="2" refY="1.25" orient="auto" markerUnits="strokeWidth">'+
						'			<circle cx="1.3" cy="1.3" r="1.3" stroke="none"  fill="'+element.background.stroke.color+'" />'+
						'     </marker>'+

						'	<marker id="circle-dest-'+element.id+'" markerWidth="5" markerHeight="5" refX="0.6" refY="1.25" orient="auto" markerUnits="strokeWidth">'+
						'			<circle cx="1.3" cy="1.3" r="1.3" stroke="none"  fill="'+element.background.stroke.color+'"/>'+
						'     </marker>'+	
						'	<marker id="square-'+element.id+'" refX="5.0" refY="5" viewBox="0 0 5 12" markerWidth="5" markerHeight="5" orient="auto" markerUnits="strokeWidth" >'+
						'	      <path d="M0 0h10v10h-10z" fill="'+element.background.stroke.color+'" />'+
						'	    </marker>'+	
						'	<marker id="diamond-'+element.id+'" refX="6.2" refY="5" viewBox="0 0 5 12" markerWidth="5" markerHeight="4.1" orient="auto" markerUnits="strokeWidth" >'+
						'	      <path d="m5 0l5 5l-5 5l-5 -5l5 -5z" fill="'+element.background.stroke.color+'" />'+
						'	    </marker>'+
						'	<marker id="arrow-line-dest-'+element.id+'" viewBox="-1 0 12 10" refX="9" refY="5" markerUnits="strokeWidth" stroke="'+element.background.stroke.color+'" markerWidth="8" markerHeight="8" orient="auto">'+
						'		<path d="M 3 1 L 10 5 L 3 9" fill="transparent"/>'+
						'	</marker>'+
						'		<marker id="arrow-line-origin-'+element.id+'" viewBox="-1 0 12 10" refX="1" refY="5" markerUnits="strokeWidth" stroke="'+element.background.stroke.color+'" markerWidth="8" markerHeight="8" orient="auto">'+
						'			<path d="M 7 1 L 0 5 L 7 9" fill="transparent"/>'+
						'		</marker>'+
						shadowDef +
					 ' </defs>'+
					
					

						lineBackground +
				  '	</svg>';
	}
	
	return shapeXml;

}

/*
For sloid Arrow
x1 = stroke-width*5
x2 = width - stroke-width*5
y1,y2 = stroke-width *2.5    
height = 	width - stroke-width*5

//circle
x= troke-width*3
y1,y2 = stroke-width *2.5    
height = 	width - stroke-width*5
*/
SlideBuilder.prototype.buildLineWithEndType = function(element){
	
	var x1 = 0;
	//5 is the marker width of end point,  we need to reduce the width of line
	//so the marker is seen in proper shape
	var buffer = 0;//Number(element.background.stroke.weight)/4;
	var y1 = Number(element.background.stroke.weight)*2;
	var y2 = y1;
	var x2 = Number(element.frame.width);
	var origin,dest = "";
	var linePad = Number(element.background.stroke.weight)*2;
	
	
	origin = this.buildLineOrigin(element);
	
	if(origin != "") {
		x1 = x1 + linePad;
		origin = 'marker-start="'+origin+'"';
	}
	
	dest = this.buildLineDest(element);
		
	if(dest != "") {
		x2 = x2 - linePad;
		dest = 'marker-end="'+dest+'"';
	}
	

	
	return '<line x1="'+x1+'" y1="'+y1+'"  x2="'+x2+'" y2="'+y2+'"  stroke="'+element.background.stroke.color+'" stroke-width="'+element.background.stroke.weight+'" '+origin+' '+dest+'  shape-rendering="crispEdges"/>';
}



SlideBuilder.prototype.buildLineOrigin = function(element){
	var origin = "";
	
	if(element.background.stroke.originEndType == this.LineEndType.END_CIRCLE){
		origin = 'url(#circle-origin-'+element.id+')';
	}
	else if(element.background.stroke.originEndType == this.LineEndType.ARROW_SOLID){
			origin = 'url(#arrow-solid-origin-'+element.id+')';
	}
	else if(element.background.stroke.originEndType == this.LineEndType.ARROW_LINE){
			origin = 'url(#arrow-line-origin-'+element.id+')';
	}
	else if(element.background.stroke.originEndType == this.LineEndType.ARROW_COMPLEX){
			origin = 'url(#arrow-line-origin-'+element.id+')';
	}
	else if(element.background.stroke.originEndType == this.LineEndType.END_SQUARE){
			origin = 'url(#square-'+element.id+')';
	}
	else if(element.background.stroke.originEndType == this.LineEndType.END_DIAMOND){
			origin = 'url(#diamond-'+element.id+')';
	}
	return origin;
}

SlideBuilder.prototype.buildLineDest = function(element){
	var dest = "";
	
	if(element.background.stroke.destEndType == this.LineEndType.END_CIRCLE){
		dest = 'url(#circle-dest-'+element.id+')';
	}
	else if(element.background.stroke.destEndType == this.LineEndType.ARROW_SOLID){
		dest = 'url(#arrow-solid-dest-'+element.id+')';
	}
	else if(element.background.stroke.destEndType == this.LineEndType.ARROW_LINE){
		dest = 'url(#arrow-line-dest-'+element.id+')';
	}
	else if(element.background.stroke.destEndType == this.LineEndType.ARROW_COMPLEX){
		dest = 'url(#arrow-line-dest-'+element.id+')';
	}
	else if(element.background.stroke.destEndType == this.LineEndType.END_SQUARE){
		dest = 'url(#square-'+element.id+')';
	}
	else if(element.background.stroke.destEndType == this.LineEndType.END_DIAMOND){
		dest = 'url(#diamond-'+element.id+')';
	}
	return dest;
}


//////////SlideInfo class ///////////////////

var SlideInfo = function(slide) { 
	
	this.slide = slide;
};


SlideInfo.prototype.numClickTransitions = NaN;
SlideInfo.prototype.numAllTransitions = NaN;
SlideInfo.prototype.slide = null;
SlideInfo.prototype.playTransition = true;
SlideInfo.prototype.numClicks = 0;
SlideInfo.prototype.resourcesLoaded = false;
SlideInfo.prototype.numStateTransitions = 0;
     


SlideInfo.prototype.hasPendingClicks = function(){
	 if (isNaN(this.numClickTransitions))
        {
            this.initializeActions();
        }
        return this.numClicks < this.numClickTransitions;
}

SlideInfo.prototype.reset = function() {
    this.numClickTransitions = NaN;
	this.numAllTransitions = NaN;
	this.numClicks = 0;

}

SlideInfo.prototype.initializeActions = function() {
      this.calcActionStateTransitions();

}

SlideInfo.prototype.calcActionStateTransitions = function(){
	 if (this.slide)
        {
			var numClicksTrans = 0;
			var numAllTrans = 0;
           
            for (element in this.slide.actions)
            {   
				 var eleAction = this.slide.actions[element];
				 for (action in eleAction)
		            {
						action = eleAction[action];
               			 switch(action.transition)
			                {
			                    case ActionState.TRANSITION_WITH_PREVIOUS:
			                    {
			                        break;
			                    }
			                    case ActionState.TRANSITION_ON_CLICK:
			                    {
			                        numClicksTrans = numClicksTrans + 1;
			                    }
			                    case ActionState.TRANSITION_ON_DELAY:
			                    {
			                        numAllTrans = numAllTrans + 1;
			                    }
			                    default:
			                    {
			                        break;
			                    }
			                }
					}
            }
            this.numClickTransitions = numClicksTrans;
            this.numAllTransitions = numAllTrans;
		}
}

////////////////////

var buildPresentation = function(xml){
	var result = [];
		Deck = { 
			id : "",
		    slides : [],
		    slideIndex : {}
		};

		Slide = {
				actions : [],
			 	elements : []
		};

		ActionState = {

		};

		Fonts = [];
		parseCustomShowXML(xml);
		
		

	   	slideBuilder = new SlideBuilder(new ActionStateManager());

        var presentationHTML  = slideBuilder.create();

		var app_css = '<link rel="stylesheet" type="text/css" href="'+STATIC_CND_PATH+'css/app.css" />';
		var chart_js = '<script src="'+STATIC_CND_PATH+'reveal/plugin/chart/Chart.min.js"></script>';
			chart_js += '<script src="'+STATIC_CND_PATH+'reveal/plugin/chart/chartjs-datalabels.js"></script>';
		var fonts_css = '';

			var cssfiles = [];
			for (var fname in Fonts) {
				fname = fname.replace(" ","%20").replace(" ","%20");
				fname = FONTS_PATH+fname+'.css';
				fonts_css += '<link rel="stylesheet" type="text/css" href="'+fname+'" />';
			}



			result["html"]  = '<html><head><meta http-equiv="Content-Type" content="text/html; charset=utf-8" />'+chart_js+app_css+fonts_css+'<style>.slideContainer{position:fixed !important;page-break-after:always !important;clip-path: inset(0 0 0 0);} .print {overflow:auto;} .componentContainer{position:fixed !important;}</style></head><body class="print" style="margin:0px;padding:0px;transform: translateZ(0);height:'+slideBuilder.fixedVerticalPosition+'px">'+presentationHTML+'</body></html>'+
			'<script type="text/javascript"> '+registerCustomCharts()+' var canvases=document.querySelectorAll("canvas");for(var i=0;i<canvases.length;i++){if(canvases[i].hasAttribute("data-chart")){if(canvases[i].hasAttribute("data-chart")){var chart=JSON.parse(canvases[i].getAttribute("data-chart"));var ctx=canvases[i].getContext("2d");new Chart(ctx,{type:chart.type,data:chart.data,options:chart.options})}}}</script>';
			
	   result["deck"] = Deck;

		return result;
}

var init = function(staticCndPath, mediaCdnPath, fontCdnPath) {

    STATIC_CND_PATH = staticCndPath;
    MEDIA_CDN_PATH = mediaCdnPath;
    FONTS_PATH = fontCdnPath;
}

var registerCustomCharts = function(){
	return 'var ShadowLineElement=Chart.elements.Line.extend({draw:function(){var t=this._chart.chart.ctx,a=t.stroke;t.stroke=function(){t.save(),t.shadowColor="#AEAEAE",t.shadowBlur=4,t.shadowOffsetX=0,t.shadowOffsetY=4,a.apply(this,arguments),t.restore()},Chart.elements.Line.prototype.draw.apply(this,arguments),t.stroke=a}});Chart.defaults.ShadowLine=Chart.defaults.line,Chart.controllers.ShadowLine=Chart.controllers.line.extend({datasetElementType:ShadowLineElement});var ShadowBarElement=Chart.elements.Rectangle.extend({draw:function(){ctx=this._chart.ctx;var t=ctx.stroke;this._chart.ctx.shadowColor="#A0A0A0",this._chart.ctx.shadowBlur=2,this._chart.ctx.shadowOffsetX=1,this._chart.ctx.shadowOffsetY=2,Chart.elements.Rectangle.prototype.draw.apply(this,arguments),this._chart.ctx.stroke=t}});Chart.defaults.ShadowBar=Chart.defaults.bar,Chart.controllers.ShadowBar=Chart.controllers.bar.extend({dataElementType:ShadowBarElement});var ShadowHorizontalBarElement=Chart.elements.Rectangle.extend({draw:function(){ctx=this._chart.ctx;var t=ctx.stroke;this._chart.ctx.shadowColor="#A0A0A0",this._chart.ctx.shadowBlur=4,this._chart.ctx.shadowOffsetX=2,this._chart.ctx.shadowOffsetY=2,Chart.elements.Rectangle.prototype.draw.apply(this,arguments),this._chart.ctx.stroke=t}});Chart.defaults.ShadowHorizontalBar=Chart.defaults.horizontalBar,Chart.controllers.ShadowHorizontalBar=Chart.controllers.horizontalBar.extend({dataElementType:ShadowHorizontalBarElement});var ShadowArcElement=Chart.elements.Arc.extend({draw:function(){ctx=this._chart.ctx,this._view.borderWidth=0,this._chart.ctx.shadowColor="#ffffff",this._chart.ctx.shadowBlur=0,this._chart.ctx.shadowOffsetX=4,this._chart.ctx.shadowOffsetY=-4,Chart.elements.Arc.prototype.draw.apply(this,arguments),this._chart.ctx.shadowColor="#00ffff",this._chart.ctx.shadowBlur=0,this._chart.ctx.shadowOffsetX=0,this._chart.ctx.shadowOffsetY=0}});Chart.defaults.ShadowPie=Chart.defaults.pie,Chart.controllers.ShadowPie=Chart.controllers.pie.extend({dataElementType:ShadowArcElement});';
}


var STATIC_CND_PATH = "https://cf-h.app.cs.cc/static/";
var MEDIA_CDN_PATH = "https://d1ghwmstyhu9mq.cloudfront.net/";
var FONTS_PATH = "https://d1ghwmstyhu9mq.cloudfront.net/font/";

var PRINT_MODE = "PRINT_MODE";


var parser = require('./xml2jsWrapper.js');
var _ = require('underscore');
const jsdom = require('jsdom').jsdom;
global.document = jsdom('');
global.window = document.defaultView;
window.console = global.console;
var $ = require('jquery');

module.exports.buildPresentation = buildPresentation;
module.exports.init = init;
