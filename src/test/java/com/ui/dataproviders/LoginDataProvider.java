package com.ui.dataproviders;

import com.ui.pojo.User;
import com.utility.CSVReaderUtility;
import com.utility.ExcelReaderUtility;
import com.utility.JSONReaderUtility;

import java.util.Iterator;
import org.testng.annotations.DataProvider;


/*Data provider 
 * can return data in object[]
 * can return data in obj[][]
 * can return data in Iterator
 * */

public class LoginDataProvider {

    @DataProvider(name = "LoginTestJsonDataProvider")
    public Iterator<User> loginTestJsonDataProvider() {
        return JSONReaderUtility.readJSONFile("loginData.json");
   }
    
    
    @DataProvider(name = "LoginTestCSVDataProvider")
    public Iterator<User> loginCSVDataProvider() {
    	return CSVReaderUtility.readCSVFile("loginData.csv");
    }
    
    @DataProvider(name = "LoginTestExcelDataProvider")
    public Iterator<User> loginExcelDataProvider() {
    	return ExcelReaderUtility.readExcleFile("loginData.xlsx");
    }
    
}
