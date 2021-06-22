package com.dc.videojc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * descriptions...
 * @author Diamon.Cheng
 * @date 2021/6/22
 */
public class ConcurrentListTest {
    public static void main(String[] args) {
        System.out.println("////Array list");
        try {
            List<Integer> list = Collections.synchronizedList(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
            for (Iterator<Integer> iterator = list.iterator(); iterator.hasNext(); ) {
                list.add(7);
                //iterator.remove();
                Integer integer = iterator.next();
                System.out.println(integer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("////Linked list");
        try {
            List<Integer> list = Collections.synchronizedList(new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6)));
            for (Iterator<Integer> iterator = list.iterator(); iterator.hasNext(); ) {
                list.add(7);
                //iterator.remove();
                Integer integer = iterator.next();
                System.out.println(integer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("////ConcurrentLinkedQueue list");
        try {
            Collection<Integer> list = new ConcurrentLinkedQueue<>(Arrays.asList(1, 2, 3, 4, 5, 6));
            for (Iterator<Integer> iterator = list.iterator(); iterator.hasNext(); ) {
                //iterator.remove();
                Integer integer = iterator.next();
                if (integer == 1) {
                    list.add(7);
                }
                if (integer > 4) {
                    iterator.remove();
                }
                System.out.println(integer);
            }
            System.out.println(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
